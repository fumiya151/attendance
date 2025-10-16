package com.example.attendance.attendance_app.controller;

import com.example.attendance.attendance_app.dto.DailyAttendanceSummaryDto;
import com.example.attendance.attendance_app.dto.PayrollDto;
import com.example.attendance.attendance_app.service.DailyAttendanceSummaryService;
import com.example.attendance.attendance_app.service.AttendancePdfService;
import com.example.attendance.attendance_app.service.PayrollPdfService;
import com.example.attendance.attendance_app.service.PayrollService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 各種PDFファイルの出力に関するAPIを一元的に処理するコントローラーです。
 * * 【責務】
 * 勤怠データや給与計算結果に基づき、PDFファイルを生成し、クライアントに返却します。
 */
@RestController
@RequestMapping("/api/exports/pdf") // ベースパスを /api/exports/pdf に統一
@RequiredArgsConstructor
public class PdfExportController {

    // 勤怠関連
    private final DailyAttendanceSummaryService summaryService;
    private final AttendancePdfService attendancePdfService;

    // 給与関連
    private final PayrollService payrollService;
    private final PayrollPdfService payrollPdfService;

    // ------------------------------------------------------------------------------------------------
    // A. 勤怠サマリー（月次勤務表）PDF出力 - 旧 AttendancePdfController のロジック
    // ------------------------------------------------------------------------------------------------

    /**
     * GET /api/exports/pdf/summaries
     * 月度勤務表PDFを生成し、ダウンロードします。（単一従業員・単一月）
     */
    @GetMapping("/summaries")
    public ResponseEntity<byte[]> exportAttendanceSummaryPdf(
            @RequestParam("employeeId") String employeeId,
            @RequestParam("yearMonth") String yearMonthStr) {

        try {
            YearMonth yearMonth = YearMonth.parse(yearMonthStr);

            List<DailyAttendanceSummaryDto> summaries = summaryService
                    .findSummariesByEmployeeAndMonth(employeeId, yearMonth);

            if (summaries.isEmpty()) {
                return createErrorResponse("対象の勤怠データが見つかりませんでした。", HttpStatus.NOT_FOUND);
            }

            byte[] pdfBytes = attendancePdfService.generateAttendancePdf(summaries);

            String baseFilename = summaries.get(0).getEmployeeName() + "_" + yearMonthStr + "_勤務表.pdf";
            return createPdfResponse(pdfBytes, baseFilename);

        } catch (DateTimeParseException e) {
            return createErrorResponse("日付形式エラー: yearMonthはYYYY-MM形式で指定してください。", HttpStatus.BAD_REQUEST);
        } catch (IOException e) {
            System.err.println("PDF生成中の致命的なエラー: " + e.getMessage());
            return createErrorResponse("PDF生成エラー: サーバーでエラーが発生しました。", HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            System.err.println("予期せぬエラー: " + e.getMessage());
            return createErrorResponse("予期せぬエラーが発生しました。", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // ------------------------------------------------------------------------------------------------
    // B. 給与明細 PDF出力 - 全従業員分 (パス: /payroll)
    // ------------------------------------------------------------------------------------------------

    /**
     * GET /api/exports/pdf/payroll
     * 指定期間の給与計算を実行し、結果の給与明細PDFファイル (全従業員) を返却します。
     */
    @GetMapping("/payroll") // 全従業員分
    public ResponseEntity<byte[]> exportPayrollPdf(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        try {
            List<PayrollDto> payrolls = payrollService.calculatePayroll(startDate, endDate);

            if (payrolls.isEmpty()) {
                return createErrorResponse(
                        "対象期間 (" + startDate + "〜" + endDate + ") の給与計算対象データが見つかりませんでした。",
                        HttpStatus.NOT_FOUND);
            }

            byte[] pdfBytes = payrollPdfService.generatePayrollPdf(payrolls, startDate, endDate);

            String baseFilename = startDate.toString() + "_" + endDate.toString() + "_給与明細(全従業員).pdf";
            return createPdfResponse(pdfBytes, baseFilename);

        } catch (IOException e) {
            System.err.println("給与明細PDF生成中の致命的なエラー: " + e.getMessage());
            return createErrorResponse("PDF生成エラー: サーバーでエラーが発生しました。", HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            System.err.println("予期せぬエラー: " + e.getMessage());
            return createErrorResponse("予期せぬエラーが発生しました。", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // ------------------------------------------------------------------------------------------------
    // C. 給与明細 PDF出力 - 個別従業員分 (パス: /payroll/single)
    // ------------------------------------------------------------------------------------------------

    /**
     * GET /api/exports/pdf/payroll/single
     * 指定期間と指定従業員の給与計算を実行し、結果の給与明細PDFファイル (個別従業員) を返却します。
     * * @param employeeId 対象従業員ID
     * 
     * @param startDate 計算開始日
     * @param endDate   計算終了日
     * @return 生成された給与明細PDFファイル (byte[])
     */
    @GetMapping("/payroll/single") // 個別従業員分
    public ResponseEntity<byte[]> exportSinglePayrollPdf(
            @RequestParam("employeeId") String employeeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        try {
            // 1. まず給与計算を実行（全従業員を対象として、結果から該当者1名分を抽出）
            List<PayrollDto> allPayrolls = payrollService.calculatePayroll(startDate, endDate);

            // 従業員IDでフィルタリング
            List<PayrollDto> singlePayroll = allPayrolls.stream()
                    .filter(p -> p.getEmployeeId().equals(employeeId))
                    .collect(Collectors.toList());

            if (singlePayroll.isEmpty()) {
                return createErrorResponse(
                        "対象の従業員ID (" + employeeId + ") の給与計算対象データが見つかりませんでした。",
                        HttpStatus.NOT_FOUND);
            }

            // 2. 抽出した1名分のリストをPDFサービスに渡してPDFを生成
            byte[] pdfBytes = payrollPdfService.generatePayrollPdf(singlePayroll, startDate, endDate);

            // 3. レスポンスヘッダーの設定 (ダウンロード用)
            String employeeName = singlePayroll.get(0).getEmployeeName();
            String yearMonthStr = startDate.toString().substring(0, 7);
            String filename = employeeName + "_" + yearMonthStr + "_給与明細.pdf";
            return createPdfResponse(pdfBytes, filename);

        } catch (IOException e) {
            System.err.println("個別給与明細PDF生成中の致命的なエラー: " + e.getMessage());
            return createErrorResponse("PDF生成エラー: サーバーでエラーが発生しました。", HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            System.err.println("予期せぬエラー: " + e.getMessage());
            return createErrorResponse("予期せぬエラーが発生しました。", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // ------------------------------------------------------------------------------------------------
    // D. ヘルパーメソッド
    // ------------------------------------------------------------------------------------------------

    /**
     * エラーレスポンスを生成するヘルパーメソッド
     */
    private ResponseEntity<byte[]> createErrorResponse(String message, HttpStatus status) {
        return ResponseEntity.status(status)
                .body(message.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * PDFレスポンスを生成するヘルパーメソッド
     */
    private ResponseEntity<byte[]> createPdfResponse(byte[] pdfBytes, String baseFilename) throws IOException {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);

        // ファイル名エンコード (日本語ファイル名対応)
        String encodedFilename = URLEncoder.encode(baseFilename, StandardCharsets.UTF_8.toString())
                .replaceAll("\\+", "%20");
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFilename);
        headers.setContentLength(pdfBytes.length);

        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }
}