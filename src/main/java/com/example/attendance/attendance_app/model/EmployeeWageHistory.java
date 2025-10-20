package com.example.attendance.attendance_app.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * 従業員の時給履歴を保持するエンティティです。
 *
 * 【用途】
 * 給与計算サービスにおいて、特定の日に適用されていた正確な時給を検索するために利用されます。
 * 時給の変更履歴を追跡し、監査に対応します。
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "employee_wage_history")
public class EmployeeWageHistory {

    /**
     * 主キー（自動採番）。
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 従業員エンティティへの参照（外部キー）。
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    /**
     * 適用される時給額。
     */
    @Column(name = "hourly_wage", nullable = false)
    private BigDecimal hourlyWage;

    /**
     * 時給が有効になる開始日。
     */
    @Column(name = "effective_start_date", nullable = false)
    private LocalDate effectiveStartDate;

    /**
     * 時給の有効期限終了日（NULLの場合は無期限）。
     */
    @Column(name = "effective_end_date")
    private LocalDate effectiveEndDate;

    /**
     * 契約タイプや雇用形態（例：正社員、パート、契約社員）。
     */
    @Column(name = "contract_type", length = 50)
    private String contractType;

    /**
     * レコード作成日時。
     */
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    /**
     * エンティティが永続化される直前に実行されるコールバックメソッド。
     *
     * 【機能】
     * `createdAt` フィールドがNULLの場合、現在時刻（タイムゾーン情報付き）で初期設定します。
     */
    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = OffsetDateTime.now();
        }
    }
}