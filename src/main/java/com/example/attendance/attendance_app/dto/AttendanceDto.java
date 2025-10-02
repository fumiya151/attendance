package com.example.attendance.attendance_app.dto;

import lombok.Data;
import java.time.OffsetDateTime;

@Data
public class AttendanceDto {
    private Long id;
    private Long employeeId;
    private OffsetDateTime stampTime;
    private String stampType;
    private String note;
}
