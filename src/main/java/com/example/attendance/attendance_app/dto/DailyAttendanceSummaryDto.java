package com.example.attendance.attendance_app.dto;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 日次勤怠実績の集計結果とステータスを保持するDTOです。
 *
 * 【用途】
 * 勤怠ログ一覧表示、詳細データ取得、勤怠修正リクエスト、およびPDF出力データとして利用されます。
 */
@Data
public class DailyAttendanceSummaryDto {
    /**
     * 主キーとなる勤怠サマリーID。
     */
    private Long id;

    /**
     * 従業員ID。
     */
    private String employeeId;

    /**
     * 従業員名（表示用）。
     */
    private String employeeName;

    /**
     * 勤務日。
     */
    private LocalDate workDate;

    /**
     * 実際の出勤時刻。
     */
    private LocalTime actualInTime;

    /**
     * 実際の退勤時刻。
     */
    private LocalTime actualOutTime;

    /**
     * 総休憩時間（分）。
     */
    private Integer totalBreakMinutes;

    /**
     * 総実労働時間（分）。
     */
    private Integer totalWorkMinutes;

    /**
     * 承認ステータス（例: PENDING, APPROVED, FINALIZED）。
     */
    private String approvalStatus;
}