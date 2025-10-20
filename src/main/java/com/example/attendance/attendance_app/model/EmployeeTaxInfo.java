package com.example.attendance.attendance_app.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 従業員の税金関連情報を保持するエンティティです。
 *
 * 【用途】
 * 給与計算サービス（特に源泉所得税や住民税の計算）で使用される、
 * 扶養人数や住民税の特別徴収額などの静的なデータを管理します。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "employee_tax_info")
public class EmployeeTaxInfo {

    /**
     * 主キー（従業員ID）：従業員テーブルの外部キーを兼ねる。
     */
    @Id
    @Column(name = "employee_id", nullable = false)
    private String employeeId;

    /**
     * 扶養人数 (所得税の源泉徴収税額表に使用)。
     */
    @Column(name = "dependent_count", nullable = false)
    private Integer dependentCount = 0;

    /**
     * 住民税 特別徴収額 (月額)。
     */
    @Column(name = "monthly_resident_tax", precision = 10, scale = 0, nullable = false)
    private BigDecimal monthlyResidentTax = BigDecimal.ZERO;

    /**
     * 住民税 適用開始月 (年度途中の入社などで使用)。
     */
    @Column(name = "resident_tax_start_month")
    private LocalDate residentTaxStartMonth;

    /**
     * 適用開始日 (この設定が有効になった日)。
     */
    @Column(name = "effective_date", nullable = false)
    private LocalDate effectiveDate = LocalDate.now();

    /**
     * 備考。
     */
    @Column(name = "notes")
    private String notes;
}