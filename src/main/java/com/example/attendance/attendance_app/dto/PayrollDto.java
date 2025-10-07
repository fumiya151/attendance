package com.example.attendance.attendance_app.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PayrollDto {
    private String employeeId;
    private String employeeName;
    private Double totalHours;
    private Double calculatedSalary;
}
