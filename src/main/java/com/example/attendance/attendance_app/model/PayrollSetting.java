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
 * 給与計算に使用する各種の設定値（料率、定額など）を保持するエンティティです。
 *
 * 【用途】
 * 法改正や社内規定の変更に対応するため、設定値をキーと期間で管理するのに利用されます。
 * （例: 雇用保険料率、残業割増率など）。
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "payroll_settings")
public class PayrollSetting {

    /**
     * 主キー：設定を一意に識別するためのキー（例: HEALTH_INSURANCE_RATE, OVERTIME_RATE）。
     */
    @Id
    @Column(name = "setting_key", nullable = false)
    private String settingKey;

    /**
     * 設定値（料率や金額など）。
     * 精度：10桁、小数点以下5桁まで保持されます。
     */
    @Column(name = "setting_value", nullable = false, precision = 10, scale = 5)
    private BigDecimal settingValue;

    /**
     * 設定が有効になる適用開始日。
     */
    @Column(name = "effective_start_date", nullable = false)
    private LocalDate effectiveStartDate;

    /**
     * 設定の適用終了日。
     * （NULLの場合は現在適用中、または無期限を意味します。）
     */
    @Column(name = "effective_end_date")
    private LocalDate effectiveEndDate;

    /**
     * 設定内容の簡単な説明。
     */
    @Column(name = "description")
    private String description;
}