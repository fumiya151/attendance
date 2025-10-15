package com.example.attendance.attendance_app.service;

import com.example.attendance.attendance_app.dto.DailyAttendanceSummaryDto;

// --- iText関連のimport ---
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.layout.element.Cell;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AttendancePdfService {

    /**
     * 勤怠サマリーリストから月次勤務表形式のPDFファイルを生成します。
     *
     * 【機能】
     * 渡された DailyAttendanceSummaryDto のリストに基づき、
     * 月次出勤簿フォーマットのPDF（タイトルと月次テーブル）を生成し、バイト配列で返却します。
     *
     * 【前提】
     * 渡されるリストは、単一の従業員、単一の月度のデータであること。
     *
     * @param summaries PDFに出力する勤怠サマリーDTOのリスト
     * @return 生成されたPDFのバイト配列
     *
     * @throws IOException フォントの読み込みまたはPDF書き込みエラー
     */
    public byte[] generateAttendancePdf(List<DailyAttendanceSummaryDto> summaries) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);

        // --- 1. 日本語フォントの設定 ---
        PdfFont japaneseFont;
        // プロジェクトのリソースパスからフォントファイルを読み込む
        String FONT_PATH = "src/main/resources/ipaexg.ttf";

        try {
            japaneseFont = PdfFontFactory.createFont(FONT_PATH, PdfEncodings.IDENTITY_H);
        } catch (IOException e) {
            System.err.println("日本語フォントの読み込みに失敗しました。プロジェクトのリソースに " + FONT_PATH + " が存在するか確認してください。");
            // 予備: 標準フォントを使用 (日本語部分は文字化けします)
            japaneseFont = PdfFontFactory.createFont(StandardFonts.HELVETICA);
        }

        document.setFont(japaneseFont).setFontSize(10);

        // --- 2. データの事前準備 ---
        if (summaries.isEmpty()) {
            document.add(new Paragraph("対象となる勤怠データがありません。"));
            document.close();
            return baos.toByteArray();
        }

        // 月と従業員名を取得
        String employeeName = summaries.get(0).getEmployeeName();
        YearMonth targetMonth = YearMonth.from(summaries.get(0).getWorkDate());
        int daysInMonth = targetMonth.lengthOfMonth();

        // 日付をキーとしてデータをマップ化（検索高速化のため）
        Map<LocalDate, DailyAttendanceSummaryDto> summaryMap = summaries.stream()
                .collect(Collectors.toMap(DailyAttendanceSummaryDto::getWorkDate, dto -> dto));

        // --- 3. タイトルと情報の追加 ---
        document.add(new Paragraph(targetMonth.getYear() + "年" + targetMonth.getMonthValue() + "月度 勤務表")
                .setFontSize(16).setBold());
        document.add(new Paragraph("氏名: " + employeeName).setFontSize(12));
        document.add(new Paragraph("出力日: " + LocalDate.now().toString()).setFontSize(8));
        document.add(new Paragraph(" ")); // スペーサー

        // --- 4. 勤務表テーブルの作成 ---
        // 8列: 日付, 曜日, 出勤, 退勤, 休憩, 実働, 残業, 備考/承認
        float[] columnWidths = { 1.5f, 1f, 2f, 2f, 1.5f, 2f, 2f, 2.5f };
        Table table = new Table(UnitValue.createPercentArray(columnWidths));
        table.setWidth(UnitValue.createPercentValue(100));

        // ヘッダー行
        String[] headers = { "日付", "曜日", "出勤時刻", "退勤時刻", "休憩(分)", "実働時間", "残業時間", "備考/承認" }; // 休憩時間も分表示に戻しました
        for (String header : headers) {
            table.addHeaderCell(new Paragraph(header)
                    .setFont(japaneseFont)
                    .setFontSize(9)
                    .setBold());
        }

        // データ行: 月の最初の日から最後の日までループ
        long totalWorkMinutesSum = 0;
        long totalOvertimeMinutesSum = 0;

        for (int day = 1; day <= daysInMonth; day++) {
            LocalDate currentDate = targetMonth.atDay(day);
            DayOfWeek dayOfWeek = currentDate.getDayOfWeek();
            DailyAttendanceSummaryDto dailyData = summaryMap.get(currentDate);

            // 日付と曜日のセル
            table.addCell(String.valueOf(currentDate.getDayOfMonth()));
            table.addCell(dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.JAPAN));

            if (dailyData != null) {
                // データが存在する場合
                long totalWorkMinutes = dailyData.getTotalWorkMinutes() != null ? dailyData.getTotalWorkMinutes() : 0;
                long overtimeMinutes = Math.max(totalWorkMinutes - (8 * 60), 0); // 標準8時間 (480分) 超過分

                totalWorkMinutesSum += totalWorkMinutes;
                totalOvertimeMinutesSum += overtimeMinutes;

                String approvalStatus = dailyData.getApprovalStatus();
                String approvalText = "PENDING".equals(approvalStatus) ? "承認待ち"
                        : ("APPROVED".equals(approvalStatus) || "FINALIZED".equals(approvalStatus)) ? "承認済" : "---";

                // ★ 実働時間と残業時間を「X.Y時間」形式に変換して表示 ★
                String formattedWorkTime = formatMinutesToDecimalHours(totalWorkMinutes) + "時間";
                String formattedOvertime = formatMinutesToDecimalHours(overtimeMinutes) + "時間";

                table.addCell(formatTime(dailyData.getActualInTime()));
                table.addCell(formatTime(dailyData.getActualOutTime()));
                table.addCell(
                        dailyData.getTotalBreakMinutes() != null ? dailyData.getTotalBreakMinutes() + "分" : "---"); // 休憩時間は分単位のまま
                table.addCell(totalWorkMinutes > 0 ? formattedWorkTime : "---"); // 修正適用
                table.addCell(overtimeMinutes > 0 ? formattedOvertime : "---"); // 修正適用
                table.addCell(approvalText);
            } else {
                // データが存在しない日（未出勤、公休など）
                table.addCell("").addCell("").addCell("").addCell("").addCell("").addCell("");
            }
        }

        // 5. 集計行の追加
        // ★ 月次合計も「X.Y時間」形式に変換して表示 ★
        String formattedTotalWorkSum = formatMinutesToDecimalHours(totalWorkMinutesSum) + "時間";
        String formattedTotalOvertimeSum = formatMinutesToDecimalHours(totalOvertimeMinutesSum) + "時間";

        table.addCell(new Cell(1, 5).add(new Paragraph("月次合計").setBold()));
        table.addCell(new Paragraph(formattedTotalWorkSum).setBold()); // 修正適用
        table.addCell(new Paragraph(formattedTotalOvertimeSum).setBold()); // 修正適用
        table.addCell(""); // 備考/承認

        document.add(table);

        document.close();
        return baos.toByteArray();
    }

    /**
     * LocalTimeをHH:mm形式に整形するヘルパーメソッド。
     */
    private String formatTime(LocalTime time) {
        if (time == null) {
            return "---";
        }
        return time.format(DateTimeFormatter.ofPattern("HH:mm"));
    }

    /**
     * 分単位の時間を「X.Y時間」の形式（小数第1位まで）に整形するヘルパーメソッド。
     *
     * @param minutes 分単位の時間
     * @return 整形された文字列（例: "140.5"）
     */
    private String formatMinutesToDecimalHours(long minutes) {
        if (minutes <= 0) {
            return "0.0";
        }
        double hours = minutes / 60.0;
        // 小数点以下第一位に四捨五入して、文字列に変換
        // 例: 140.5時間
        return String.format(Locale.US, "%.1f", hours);
    }
}