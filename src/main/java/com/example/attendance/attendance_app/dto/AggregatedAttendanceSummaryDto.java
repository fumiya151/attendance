package com.example.attendance.attendance_app.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 指定された期間における、従業員ごとの集計された勤務実績を保持するDTOです。
 *
 * 【用途】
 * 主に給与計算や管理画面での集計結果一覧表示のために利用されます。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AggregatedAttendanceSummaryDto {
    /**
     * 従業員ID。
     */
    private String employeeId;

    /**
     * 従業員名。
     */
    private String employeeName;

    /**
     * 総労働時間（時間単位、小数）。
     */
    private double totalHours;

    /**
     * 総残業時間（時間単位、小数）。
     */
    private double overtimeHours;

    /**
     * 総深夜勤務時間（時間単位、小数）。
     */
    private double lateNightHours;
}