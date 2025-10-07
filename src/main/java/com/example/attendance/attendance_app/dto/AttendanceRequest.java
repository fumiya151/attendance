package com.example.attendance.attendance_app.dto;

import lombok.Data;

@Data
public class AttendanceRequest {
    private String employeeId;
    private String name;
    private String attendanceType;
}
