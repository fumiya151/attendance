package com.example.attendance.attendance_app.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 源泉所得税の月額表（扶養控除後）の税率データを保持するエンティティです。
 *
 * 【用途】
 * 給与計算において、社会保険料控除後の金額と扶養人数に基づき、
 * 徴収すべき源泉所得税額を決定するために利用されます。
 */
@Entity
@Table(name = "income_tax_rates",
        // 適用開始日と金額の範囲が重複しないようにする制約
        uniqueConstraints = @UniqueConstraint(columnNames = { "effective_start_date", "income_from" }))
@Getter
@Setter
@ToString
public class IncomeTaxRate {

    /**
     * 主キー（自動採番）。
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 適用開始日 (税制改正対応用)。
     */
    @Column(name = "effective_start_date", nullable = false)
    private LocalDate effectiveStartDate;

    /**
     * 控除後の給与額 範囲: 〜未満 (例: 50,000)。
     */
    @Column(name = "income_from", precision = 10, scale = 0, nullable = false)
    private BigDecimal incomeFrom;

    /**
     * 控除後の給与額 範囲: 〜まで (例: 59,999)。
     */
    @Column(name = "income_to", precision = 10, scale = 0, nullable = false)
    private BigDecimal incomeTo;

    /**
     * 扶養親族 0人の場合の源泉徴収税額。
     */
    @Column(name = "tax_0", precision = 10, scale = 0, nullable = false)
    private BigDecimal tax0;

    /**
     * 扶養親族 1人の場合の源泉徴収税額。
     */
    @Column(name = "tax_1", precision = 10, scale = 0, nullable = false)
    private BigDecimal tax1;

    /**
     * 扶養親族 2人の場合の源泉徴収税額。
     */
    @Column(name = "tax_2", precision = 10, scale = 0, nullable = false)
    private BigDecimal tax2;

    /**
     * 税額表の適用区分（甲欄: KOU, 乙欄: OTSU）。
     */
    @Column(name = "tax_type", length = 4, nullable = false)
    private String taxType;
}