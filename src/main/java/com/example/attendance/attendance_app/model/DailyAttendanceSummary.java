package com.example.attendance.attendance_app.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;

/**
 * 日次勤怠実績サマリーエンティティ
 * attendanceテーブルの生ログから計算された、日次で確定する勤務実績を格納する。
 * 給与計算の基礎データとして利用される。
 */
@Entity
@Table(name = "daily_attendance_summary", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "employee_id", "work_date" }) })
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DailyAttendanceSummary {

    /**
     * 主キー
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 従業員ID (外部キー)
     */
    @Column(name = "employee_id", nullable = false, length = 20)
    private String employeeId;

    /**
     * 勤務日
     */
    @Column(name = "work_date", nullable = false)
    private LocalDate workDate;

    /**
     * 実際の最初の出勤時刻
     */
    @Column(name = "actual_in_time")
    private LocalTime actualInTime;

    /**
     * 実際の最後の退勤時刻
     */
    @Column(name = "actual_out_time")
    private LocalTime actualOutTime;

    /**
     * 実労働時間（分）
     */
    @Column(name = "total_work_minutes", nullable = false)
    private Integer totalWorkMinutes = 0;

    /**
     * 総休憩時間（分）
     */
    @Column(name = "total_break_minutes", nullable = false)
    private Integer totalBreakMinutes = 0;

    /**
     * 残業時間（分）
     */
    @Column(name = "overtime_minutes", nullable = false)
    private Integer overtimeMinutes = 0;

    /**
     * 深夜勤務時間（分）
     */
    @Column(name = "night_shift_minutes", nullable = false)
    private Integer nightShiftMinutes = 0;

    /**
     * 勤怠の確定状態（PENDING, FINALIZED, ADJUSTEDなど）
     */
    @Column(name = "status", nullable = false, length = 20)
    private String status;

    /**
     * 修正履歴ID (承認された修正がある場合)
     */
    @Column(name = "adjustment_id")
    private Long adjustmentId;

    /**
     * 最終集計日時
     */
    @Column(name = "calculated_at")
    private OffsetDateTime calculatedAt;

    /**
     * 承認操作を行った従業員ID (fk_summary_approved_by)
     */
    @Column(name = "approved_by_id", length = 20)
    private String approvedById;

    /**
     * 勤怠が承認された日時
     */
    @Column(name = "approved_at")
    private OffsetDateTime approvedAt;

    /**
     * 最終集計または修正操作を行った従業員ID (fk_summary_updated_by)
     */
    @Column(name = "updated_by_id", length = 20)
    private String updatedById;

    @PrePersist
    protected void onCreate() {
        // 新規作成時に集計日時を設定
        if (this.calculatedAt == null) {
            this.calculatedAt = OffsetDateTime.now();
        }
        // approved_atは承認操作時のみ設定されるため、ここでは設定しない
    }

    @PreUpdate
    protected void onUpdate() {
        // 更新時に集計日時を更新
        this.calculatedAt = OffsetDateTime.now();
    }
}