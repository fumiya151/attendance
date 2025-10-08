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
}
