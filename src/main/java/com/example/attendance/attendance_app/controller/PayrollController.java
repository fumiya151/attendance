package com.example.attendance.attendance_app.controller;

import com.example.attendance.attendance_app.dto.PayrollDto;
import com.example.attendance.attendance_app.service.PayrollService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/payroll")
public class PayrollController {

    @Autowired
    private PayrollService payrollService;

    @GetMapping("/calculate")
    public ResponseEntity<List<PayrollDto>> calculatePayroll() {
        List<PayrollDto> payrolls = payrollService.calculatePayroll();
        return ResponseEntity.ok(payrolls);
    }
}
