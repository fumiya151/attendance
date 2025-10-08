package com.example.attendance.attendance_app.dto;

import com.example.attendance.attendance_app.model.Employee;
import lombok.Data;

@Data
public class EmployeeRegistrationRequest {
    private String employeeId;
    private String name;
    private String department;
    private String email;
    private String password;
    private Long roleId; // 追加された役割ID
}
