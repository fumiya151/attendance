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

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "payroll_settings")
public class PayrollSetting {

    /** 設定キー (例: HEALTH_INSURANCE_RATE, OVERTIME_RATE) */
    @Id
    @Column(name = "setting_key", nullable = false)
    private String settingKey;

    /** 設定値 (料率や金額など) */
    @Column(name = "setting_value", nullable = false, precision = 10, scale = 5)
    private BigDecimal settingValue;

    /** 適用開始日 */
    @Column(name = "effective_start_date", nullable = false)
    private LocalDate effectiveStartDate;

    /** 適用終了日 (null の場合は現在適用中) */
    @Column(name = "effective_end_date")
    private LocalDate effectiveEndDate;

    /** 設定の簡単な説明 */
    @Column(name = "description")
    private String description;
}