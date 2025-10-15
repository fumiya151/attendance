package com.example.attendance.attendance_app.controller;

import com.example.attendance.attendance_app.dto.DailyAttendanceSummaryDto;
import com.example.attendance.attendance_app.service.DailyAttendanceSummaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * 勤怠サマリーのPDF出力に関するAPIを処理するコントローラーです。
 * 責務を DailyAttendanceSummaryController から分離し、保守性を向上させます。
 */
@RestController
@RequestMapping("/api/summaries")
@RequiredArgsConstructor
public class AttendancePdfController {

    // DailyAttendanceSummaryService を使用してデータを取得し、PDFServiceに渡すことを想定
    private final DailyAttendanceSummaryService summaryService;

    /**
     * GET /api/summaries/export/pdf
     * 月度勤務表PDFを生成し、ダウンロードします。
     *
     * @param employeeId     対象従業員ID
     * @param yearMonthStr 対象年月 (YYYY-MM形式)
     * @return 生成されたPDFファイル (byte[])
     */
    @GetMapping("/export/pdf")
    public ResponseEntity<byte[]> exportPdf(
            @RequestParam("employeeId") String employeeId,
            @RequestParam("yearMonth") String yearMonthStr) {

        try {
            // 1. パラメータの検証と変換
            YearMonth yearMonth = YearMonth.parse(yearMonthStr);

            // 2. Serviceから指定された従業員、月度の勤怠サマリーを取得
            List<DailyAttendanceSummaryDto> summaries = summaryService
                    .findSummariesByEmployeeAndMonth(employeeId, yearMonth);

            if (summaries.isEmpty()) {
                // データが見つからない場合は 404
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(("対象の従業員ID (" + employeeId + ") または " + yearMonthStr
                                + " の勤怠データが見つかりませんでした。")
                                .getBytes(StandardCharsets.UTF_8));
            }

            // 3. PDF生成サービスを呼び出す (summaryService経由で委譲される)
            byte[] pdfBytes = summaryService.generateAttendancePdf(summaries);

            // 4. レスポンスヘッダーの設定 (ダウンロード用)
            String baseFilename = summaries.get(0).getEmployeeName() + "_" + yearMonthStr + "_勤務表.pdf";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);

            // ファイル名エンコード (日本語ファイル名対応)
            String encodedFilename = URLEncoder.encode(baseFilename, StandardCharsets.UTF_8.toString())
                    .replaceAll("\\+", "%20");
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFilename);
            headers.setContentLength(pdfBytes.length);

            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
        } catch (DateTimeParseException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(("日付形式エラー: yearMonthはYYYY-MM形式で指定してください。")
                            .getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            // PDF生成エラー (フォントエラーなど)
            System.err.println("PDF生成中の致命的なエラー: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(("PDF生成エラー: サーバーでエラーが発生しました。詳細をサーバーログで確認してください。")
                            .getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            System.err.println("予期せぬエラー: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(("予期せぬエラーが発生しました。").getBytes(StandardCharsets.UTF_8));
        }
    }
}