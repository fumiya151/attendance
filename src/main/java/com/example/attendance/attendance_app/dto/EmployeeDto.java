package com.example.attendance.attendance_app.dto;

import lombok.Data;

@Data
public class EmployeeDto {
    private String employeeId;
    private String name;
    private String email;
    private String department;
    private boolean active;
    private Long roleId;
    private String wage;
    private Integer dependentCount; // 扶養人数（所得税計算用）
    private Double monthlyResidentTax; // 住民税月額（特別徴収）
}