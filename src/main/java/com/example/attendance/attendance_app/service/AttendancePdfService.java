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

/**
 * 勤怠サマリーリストから月次勤務表形式のPDFファイルを生成するサービスです。
 *
 * 【機能】
 * iTextライブラリを使用して、渡された勤怠サマリーデータに基づいたPDF文書を作成します。
 *
 * 【注意事項】
 * PDF内に日本語を出力するため、プロジェクトのリソースディレクトリに日本語フォントファイル（ipaexg.ttfなど）が必要です。
 */
@Service
@RequiredArgsConstructor
public class AttendancePdfService {

    // --- クラス定数定義 ---
    private static final String FONT_PATH = "src/main/resources/ipaexg.ttf";
    private static final String FONT_ERROR_MESSAGE = "日本語フォントの読み込みに失敗しました。プロジェクトのリソースに " + FONT_PATH
            + " が存在するか確認してください。";

    private static final float FONT_SIZE_BASE = 10f;
    private static final float FONT_SIZE_TITLE = 16f;
    private static final float FONT_SIZE_NAME = 12f;
    private static final float FONT_SIZE_DATE = 8f;
    private static final float FONT_SIZE_HEADER = 9f;

    private static final int STANDARD_WORK_MINUTES = 8 * 60; // 480分
    private static final String DEFAULT_TIME_DISPLAY = "---";
    private static final String FORMAT_HH_MM = "HH:mm";
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern(FORMAT_HH_MM);
    private static final String FORMAT_DECIMAL_HOURS = "%.1f";
    private static final String UNIT_MINUTES = "分";
    private static final String UNIT_HOURS = "時間";

    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_APPROVED = "APPROVED";
    private static final String STATUS_FINALIZED = "FINALIZED";
    private static final String DISPLAY_PENDING = "承認待ち";
    private static final String DISPLAY_APPROVED = "承認済";
    private static final String[] HEADERS = {
            "日付", "曜日", "出勤時刻", "退勤時刻", "休憩(" + UNIT_MINUTES + ")", "実働時間", "残業時間", "備考/承認"
    };
    private static final float[] COLUMN_WIDTHS = { 1.5f, 1f, 2f, 2f, 1.5f, 2f, 2f, 2.5f };
    private static final int COLSPAN_TOTAL_SUMMARY = 5; // ★追加定数：集計行の結合数★

    /**
     * 勤怠サマリーリストから月次勤務表形式のPDFファイルを生成します。
     *
     * 【機能】
     * 渡された DailyAttendanceSummaryDto のリストに基づき、
     * 月次出勤簿フォーマットのPDF（タイトルと月次テーブル）を生成し、バイト配列で返却します。
     *
     * 【注意事項】
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
        try {
            japaneseFont = PdfFontFactory.createFont(FONT_PATH, PdfEncodings.IDENTITY_H);
        } catch (IOException e) {
            System.err.println(FONT_ERROR_MESSAGE);
            // 予備: 標準フォントを使用 (日本語部分は文字化けします)
            japaneseFont = PdfFontFactory.createFont(StandardFonts.HELVETICA);
        }
        document.setFont(japaneseFont).setFontSize(FONT_SIZE_BASE);

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
                .setFontSize(FONT_SIZE_TITLE).setBold());
        document.add(new Paragraph("氏名: " + employeeName).setFontSize(FONT_SIZE_NAME));
        document.add(new Paragraph("出力日: " + LocalDate.now().toString()).setFontSize(FONT_SIZE_DATE));
        document.add(new Paragraph(" ")); // スペーサー

        // --- 4. 勤務表テーブルの作成 ---
        Table table = new Table(UnitValue.createPercentArray(COLUMN_WIDTHS));
        table.setWidth(UnitValue.createPercentValue(100));

        // ヘッダー行
        for (String header : HEADERS) {
            table.addHeaderCell(new Paragraph(header)
                    .setFont(japaneseFont)
                    .setFontSize(FONT_SIZE_HEADER)
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
                long overtimeMinutes = Math.max(totalWorkMinutes - STANDARD_WORK_MINUTES, 0);

                totalWorkMinutesSum += totalWorkMinutes;
                totalOvertimeMinutesSum += overtimeMinutes;

                String approvalStatus = dailyData.getApprovalStatus();
                String approvalText = STATUS_PENDING.equals(approvalStatus) ? DISPLAY_PENDING
                        : (STATUS_APPROVED.equals(approvalStatus) || STATUS_FINALIZED.equals(approvalStatus))
                                ? DISPLAY_APPROVED
                                : DEFAULT_TIME_DISPLAY;

                // 実働時間と残業時間を「X.Y時間」形式に変換して表示
                String formattedWorkTime = formatMinutesToDecimalHours(totalWorkMinutes) + UNIT_HOURS;
                String formattedOvertime = formatMinutesToDecimalHours(overtimeMinutes) + UNIT_HOURS;

                table.addCell(formatTime(dailyData.getActualInTime()));
                table.addCell(formatTime(dailyData.getActualOutTime()));
                table.addCell(
                        dailyData.getTotalBreakMinutes() != null ? dailyData.getTotalBreakMinutes() + UNIT_MINUTES
                                : DEFAULT_TIME_DISPLAY);
                table.addCell(totalWorkMinutes > 0 ? formattedWorkTime : DEFAULT_TIME_DISPLAY);
                table.addCell(overtimeMinutes > 0 ? formattedOvertime : DEFAULT_TIME_DISPLAY);
                table.addCell(approvalText);
            } else {
                // データが存在しない日（未出勤、公休など）
                table.addCell(DEFAULT_TIME_DISPLAY).addCell(DEFAULT_TIME_DISPLAY).addCell(DEFAULT_TIME_DISPLAY)
                        .addCell(DEFAULT_TIME_DISPLAY).addCell(DEFAULT_TIME_DISPLAY).addCell(DEFAULT_TIME_DISPLAY);
            }
        }

        // 5. 集計行の追加
        String formattedTotalWorkSum = formatMinutesToDecimalHours(totalWorkMinutesSum) + UNIT_HOURS;
        String formattedTotalOvertimeSum = formatMinutesToDecimalHours(totalOvertimeMinutesSum) + UNIT_HOURS;

        table.addCell(new Cell(1, COLSPAN_TOTAL_SUMMARY).add(new Paragraph("月次合計").setBold()));
        table.addCell(new Paragraph(formattedTotalWorkSum).setBold());
        table.addCell(new Paragraph(formattedTotalOvertimeSum).setBold());
        table.addCell(""); // 備考/承認

        document.add(table);

        document.close();
        return baos.toByteArray();
    }

    /**
     * LocalTimeをHH:mm形式に整形するヘルパーメソッド。
     *
     * 【機能】
     * LocalTimeオブジェクトを「時:分」（HH:mm）の文字列形式に変換します。
     *
     * 【注意事項】
     * timeがnullの場合、ハイフン3つ（---）を返します。
     *
     * @param time 整形対象の時刻
     * @return HH:mm形式の文字列、または"---"
     */
    private String formatTime(LocalTime time) {
        if (time == null) {
            return DEFAULT_TIME_DISPLAY;
        }
        return time.format(TIME_FORMATTER);
    }

    /**
     * 分単位の時間を「X.Y時間」の形式（小数第1位まで）に整形するヘルパーメソッド。
     *
     * 【機能】
     * 分単位の時間を小数付きの時間に変換し、小数点以下第一位までの形式（例: "140.5"）の文字列として返します。
     *
     * 【注意事項】
     * 0分以下の場合は "0.0" を返します。変換には四捨五入が適用されます。
     *
     * @param minutes 分単位の時間
     * @return 整形された文字列（例: "140.5"）
     */
    private String formatMinutesToDecimalHours(long minutes) {
        if (minutes <= 0) {
            return "0.0";
        }
        double hours = minutes / 60.0;
        return String.format(Locale.US, FORMAT_DECIMAL_HOURS, hours);
    }
}