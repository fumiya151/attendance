package com.example.attendance.attendance_app.controller;

import com.example.attendance.attendance_app.dto.AttendanceRequest;
import com.example.attendance.attendance_app.dto.AttendanceDto;
import com.example.attendance.attendance_app.model.Attendance;
import com.example.attendance.attendance_app.service.AttendanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;

    @PostMapping("/stamp")
    public ResponseEntity<String> recordAttendance(@RequestBody AttendanceRequest request) {
        Attendance savedAttendance = attendanceService.recordAttendance(request);

        String formattedTimestamp = savedAttendance.getStampTime()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        String message = String.format(
                "%sさんの「%s」を記録しました。(%s)",
                request.getName(), // Use name from request to avoid lazy loading issues
                savedAttendance.getStampType(),
                formattedTimestamp
        );

        return ResponseEntity.ok(message);
    }

    @GetMapping("/latest/{employeeId}")
    public ResponseEntity<AttendanceDto> getLatestAttendance(@PathVariable Long employeeId) {
        return attendanceService.getLatestAttendance(employeeId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
