package com.example.attendance.attendance_app.dto;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class DailyAttendanceSummaryDto {
    private Long id;
    private String employeeId;
    private String employeeName;
    private LocalDate workDate;

    // 勤務情報
    private LocalTime actualInTime;
    private LocalTime actualOutTime;
    private Integer totalBreakMinutes;
    private Integer totalWorkMinutes;

    // ステータス情報
    private String logStatus;
    private String approvalStatus;
}
