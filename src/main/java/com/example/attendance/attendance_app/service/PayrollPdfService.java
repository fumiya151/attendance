package com.example.attendance.attendance_app.service;

import com.example.attendance.attendance_app.dto.PayrollDto;
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
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.TextAlignment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * 給与計算結果リストから給与明細PDFファイルを生成するサービスです。
 * * 【機能】
 * iTextライブラリを使用して、渡された給与計算結果（PayrollDto）に基づいたPDF文書を作成します。
 * 各従業員はPDF内でセクション分けされます。
 * * 【注意事項】
 * PDF内に日本語を出力するため、プロジェクトのリソースディレクトリに日本語フォントファイル（ipaexg.ttfなど）が必要です。
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PayrollPdfService {

    // --- クラス定数定義 ---
    private static final String FONT_PATH = "src/main/resources/ipaexg.ttf";
    private static final String FONT_ERROR_MESSAGE = "日本語フォントの読み込みに失敗しました。プロジェクトのリソースに " + FONT_PATH
            + " が存在するか確認してください。代わりに標準フォントを使用します。";
    private static final String FORMAT_CURRENCY_DECIMAL = "%.2f";

    private static final float FONT_SIZE_BASE = 10f;
    private static final float FONT_SIZE_TITLE = 18f;
    private static final float FONT_SIZE_HEADER = 12f;

    private static final NumberFormat CURRENCY_FORMAT = NumberFormat.getCurrencyInstance(Locale.JAPAN);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    // ★修正: 支給・控除用のヘッダーを定義★
    private static final String[] ITEM_HEADERS = { "項目", "時間/回数", "単価/料率", "金額" };
    private static final float[] ITEM_COLUMN_WIDTHS = { 4f, 2f, 2f, 3f };

    // 合計行の結合数（項目〜単価/料率までを結合）
    private static final int COLSPAN_TOTAL_SUMMARY = 3;

    /**
     * 給与計算結果リストから給与明細PDFファイルを生成します。
     * ...
     */
    public byte[] generatePayrollPdf(List<PayrollDto> payrolls, LocalDate startDate, LocalDate endDate)
            throws IOException {

        // try-finally構造を使用し、リソースを明示的にクローズ
        PdfWriter writer = null;
        PdfDocument pdf = null;
        Document document = null;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try {
            writer = new PdfWriter(baos);
            pdf = new PdfDocument(writer);
            document = new Document(pdf);

            log.info("PDF生成開始: 期間 {} 〜 {}", startDate, endDate);

            document.setMargins(30, 30, 30, 30);

            // --- 1. 日本語フォントの設定 ---
            PdfFont japaneseFont;
            try {
                japaneseFont = PdfFontFactory.createFont(FONT_PATH, PdfEncodings.IDENTITY_H);
                log.info("日本語フォント ({}) の読み込みに成功しました。", FONT_PATH);
            } catch (IOException e) {
                log.warn(FONT_ERROR_MESSAGE, e);
                japaneseFont = PdfFontFactory.createFont(StandardFonts.HELVETICA);
            }

            document.setFont(japaneseFont);
            document.setFontSize(FONT_SIZE_BASE);

            // --- 2. データの事前準備 ---
            if (payrolls.isEmpty()) {
                log.warn("給与計算データが空のため、空のPDFを返します。");
                document.add(new Paragraph("対象となる給与計算データがありません。")
                        .setFontSize(FONT_SIZE_BASE).setBold());
                document.close();
                return baos.toByteArray();
            }

            // --- 3. 全従業員分の給与明細をループで作成 ---
            for (int i = 0; i < payrolls.size(); i++) {
                PayrollDto payroll = payrolls.get(i);
                log.debug("従業員 {} の明細を生成中...", payroll.getEmployeeId());

                if (i > 0) {
                    document.add(new Paragraph("\n\n"));
                }

                double totalHours = payroll.getTotalHours();
                double basePaySalary = payroll.getBasePaySalary(); // 基本給

                // ゼロ除算回避ロジック（単価）
                String baseRateDisplay;
                if (totalHours > 0) {
                    double hourlyRate = basePaySalary / totalHours;
                    String roundedRateString = String.format(Locale.US, FORMAT_CURRENCY_DECIMAL, hourlyRate);
                    baseRateDisplay = CURRENCY_FORMAT.format(Double.parseDouble(roundedRateString));
                } else {
                    baseRateDisplay = "-";
                }

                // --- ページの共通情報 ---
                document.add(new Paragraph("給与明細書")
                        .setFontSize(FONT_SIZE_TITLE)
                        .setBold()
                        .setTextAlignment(TextAlignment.CENTER));

                document.add(
                        new Paragraph(
                                "集計期間: " + startDate.format(DATE_FORMATTER) + " 〜 " + endDate.format(DATE_FORMATTER))
                                .setFontSize(FONT_SIZE_BASE)
                                .setTextAlignment(TextAlignment.CENTER));

                document.add(
                        new Paragraph("氏名: " + payroll.getEmployeeName() + " (ID: " + payroll.getEmployeeId() + ")")
                                .setFontSize(FONT_SIZE_HEADER)
                                .setBold()
                                .setMarginTop(10)
                                .setMarginBottom(10));

                // ===============================================
                // A. 支給の部テーブル
                // ===============================================
                document.add(new Paragraph("【支給項目】").setBold().setMarginTop(15));
                Table payTable = createItemTable(japaneseFont);

                // 1. 基本給 (総労働時間)
                addItemRow(payTable, "基本給",
                        String.format(Locale.US, "%.2f 時間", totalHours),
                        baseRateDisplay,
                        CURRENCY_FORMAT.format(basePaySalary));

                // 2. 時間外手当
                if (payroll.getOvertimePay() > 0) {
                    addItemRow(payTable, "時間外手当",
                            String.format(Locale.US, "%.2f 時間", payroll.getOvertimeHours()),
                            "割増分",
                            CURRENCY_FORMAT.format(payroll.getOvertimePay()));
                }

                // 3. 深夜手当
                if (payroll.getLateNightPay() > 0) {
                    addItemRow(payTable, "深夜手当",
                            String.format(Locale.US, "%.2f 時間", payroll.getLateNightHours()),
                            "割増分",
                            CURRENCY_FORMAT.format(payroll.getLateNightPay()));
                }

                // 支給合計
                addSummaryRow(payTable, "支給合計額",
                        CURRENCY_FORMAT.format(payroll.getTotalGrossPay()),
                        FONT_SIZE_HEADER);
                document.add(payTable);

                // ===============================================
                // B. 控除の部テーブル
                // ===============================================
                document.add(new Paragraph("【控除項目】").setBold().setMarginTop(15));
                Table deductionTable = createItemTable(japaneseFont);

                // 1. 健康保険料
                if (payroll.getHealthInsuranceFee() > 0) {
                    addItemRow(deductionTable, "健康保険料",
                            "-", // 時間/回数
                            "料率",
                            CURRENCY_FORMAT.format(payroll.getHealthInsuranceFee()));
                }

                // 2. 厚生年金保険料
                if (payroll.getPensionFee() > 0) {
                    addItemRow(deductionTable, "厚生年金保険料",
                            "-",
                            "料率",
                            CURRENCY_FORMAT.format(payroll.getPensionFee()));
                }

                // 3. 雇用保険料
                if (payroll.getEmploymentInsuranceFee() > 0) {
                    addItemRow(deductionTable, "雇用保険料",
                            "-",
                            "料率",
                            CURRENCY_FORMAT.format(payroll.getEmploymentInsuranceFee()));
                }

                // 控除合計
                addSummaryRow(deductionTable, "控除合計額",
                        CURRENCY_FORMAT.format(payroll.getTotalDeduction()),
                        FONT_SIZE_HEADER);
                document.add(deductionTable);

                // ===============================================
                // C. 差引支給額 (最終合計)
                // ===============================================
                document.add(new Paragraph("\n"));
                Table finalTable = new Table(UnitValue.createPercentArray(new float[] { 8f, 3f }));
                finalTable.setWidth(UnitValue.createPercentValue(100));

                Cell netPayLabel = new Cell()
                        .add(new Paragraph("差引支給額")
                                .setBold()
                                .setFontSize(FONT_SIZE_TITLE - 2))
                        .setTextAlignment(TextAlignment.RIGHT);

                Cell netPayValue = new Cell()
                        .add(new Paragraph(CURRENCY_FORMAT.format(payroll.getNetPay()))
                                .setBold()
                                .setFontSize(FONT_SIZE_TITLE))
                        .setTextAlignment(TextAlignment.RIGHT);

                finalTable.addCell(netPayLabel);
                finalTable.addCell(netPayValue);
                document.add(finalTable);
            }

            // 正常終了前に必ず明示的にクローズ
            document.close();
            log.info("PDF生成完了。サイズ: {} バイト", baos.size());
            return baos.toByteArray();

        } catch (Exception e) {
            log.error("PDF描画中に致命的な実行時エラーが発生しました。", e);
            throw new IOException("PDF生成に失敗しました: " + e.getMessage(), e);
        } finally {
            // クローズ処理を最後に実行
            if (document != null) {
                try {
                    document.close();
                } catch (Exception ignored) {
                }
            }
            if (pdf != null) {
                try {
                    pdf.close();
                } catch (Exception ignored) {
                }
            }
            if (writer != null) {
                try {
                    writer.close();
                } catch (Exception ignored) {
                }
            }
        }
    }

    /** ヘルパー: 項目テーブルのヘッダーを作成 */
    private Table createItemTable(PdfFont font) {
        Table table = new Table(UnitValue.createPercentArray(ITEM_COLUMN_WIDTHS));
        table.setWidth(UnitValue.createPercentValue(100));

        for (String header : ITEM_HEADERS) {
            table.addHeaderCell(createHeaderCell(font, header));
        }
        return table;
    }

    /** ヘルパー: 項目テーブルのデータ行を追加 */
    private void addItemRow(Table table, String item, String hours, String unitPrice, String amount) {
        table.addCell(new Cell().add(new Paragraph(item)));
        table.addCell(new Cell().add(new Paragraph(hours)).setTextAlignment(TextAlignment.RIGHT));
        table.addCell(new Cell().add(new Paragraph(unitPrice)).setTextAlignment(TextAlignment.RIGHT));
        table.addCell(new Cell().add(new Paragraph(amount)).setTextAlignment(TextAlignment.RIGHT));
    }

    /** ヘルパー: 合計行を追加 */
    private void addSummaryRow(Table table, String label, String amount, float fontSize) {
        Cell totalLabel = new Cell(1, COLSPAN_TOTAL_SUMMARY)
                .add(new Paragraph(label).setBold())
                .setFontSize(fontSize)
                .setTextAlignment(TextAlignment.RIGHT);

        Cell totalAmount = new Cell()
                .add(new Paragraph(amount).setBold())
                .setFontSize(fontSize)
                .setTextAlignment(TextAlignment.RIGHT);

        table.addCell(totalLabel);
        table.addCell(totalAmount);
    }

    // (createHeaderCell ヘルパーメソッドは変更なし)
    private Cell createHeaderCell(PdfFont font, String text) {
        return new Cell()
                .add(new Paragraph(text)
                        .setFont(font)
                        .setBold())
                .setFontSize(FONT_SIZE_HEADER - 2)
                .setTextAlignment(TextAlignment.CENTER)
                .setHorizontalAlignment(HorizontalAlignment.CENTER);
    }
}