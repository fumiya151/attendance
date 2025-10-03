package com.example.attendance.attendance_app.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.attendance.attendance_app.dto.EmployeeDto;
import com.example.attendance.attendance_app.service.EmployeeService;

import java.util.List;

@RestController
@RequestMapping("/api/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;

    /**
     * 従業員一覧取得APIのエンドポイントです.
     *
     * 【機能】
     * 従業員情報を全件取得し返却します。
     *
     *【注意事項】
     * 特になし
     *
     * @return 従業員DTOリスト
     */
    @GetMapping
    public List<EmployeeDto> getEmployees() {
        return employeeService.getEmployees();
    }
}