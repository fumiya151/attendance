package com.example.attendance.attendance_app.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AggregatedAttendanceSummaryDto {
    private String employeeId;
    private String employeeName;
    private double totalHours;
    private double overtimeHours;
    private double lateNightHours;
}
