package com.example.attendance.attendance_app.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MonthlySummaryDto {
    private double totalWorkHours;
    private double averageOvertimeHours;
}
