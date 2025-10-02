package com.example.attendance.attendance_app.controller;

import com.example.attendance.attendance_app.dto.AttendanceRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/attendance")
public class AttendanceController {

    @PostMapping("/stamp")
    public ResponseEntity<String> recordAttendance(@RequestBody AttendanceRequest request) {
        // 本来はここでデータベースに保存する処理を行いますが、
        // まずは受け取った情報を標準出力に表示します。
        System.out.println("[" + LocalDateTime.now() + "] Received attendance stamp:");
        System.out.println("  Employee ID: " + request.getEmployeeId());
        System.out.println("  Employee Code: " + request.getEmployeeCode());
        System.out.println("  Name: " + request.getName());
        System.out.println("  Type: " + request.getAttendanceType());

        String message = String.format("%sさんの「%s」を記録しました。", request.getName(), request.getAttendanceType());

        return ResponseEntity.ok(message);
    }
}
