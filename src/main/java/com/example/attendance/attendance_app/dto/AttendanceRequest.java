package com.example.attendance.attendance_app.dto;

import lombok.Data;

@Data
public class AttendanceRequest {
    private Long employeeId;
    private String employeeCode;
    private String name;
    private String attendanceType;
}
