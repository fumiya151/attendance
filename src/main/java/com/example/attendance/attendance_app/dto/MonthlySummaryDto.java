package com.example.attendance.attendance_app.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 特定の月度における勤怠集計の概要を保持するDTOです。
 *
 * 【用途】
 * 管理画面のダッシュボードやレポート機能で、月次の総労働時間や平均残業時間といったサマリーを表示するために利用されます。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MonthlySummaryDto {
    /**
     * 全従業員の月間総労働時間（時間単位、小数）。
     */
    private double totalWorkHours;

    /**
     * 全従業員の平均残業時間（時間単位、小数）。
     */
    private double averageOvertimeHours;
}