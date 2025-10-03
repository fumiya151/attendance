package com.example.attendance.attendance_app.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import com.example.attendance.attendance_app.dto.EmployeeDto;
import com.example.attendance.attendance_app.model.Employee;
import com.example.attendance.attendance_app.service.EmployeeService;

import java.util.List;

@RestController
@RequestMapping("/api/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;

    /**
     * 従業員登録APIのエンドポイントです.
     *
     * 【機能】
     * 新規従業員情報を登録します。
     *
     *【注意事項】
     * パスワードはハッシュ化されて保存されます。
     *
     * @param employee 従業員情報
     * @return 登録結果メッセージ
     */
    @PostMapping
    public String registerEmployee(@RequestBody Employee employee) {
        employeeService.registerEmployee(employee);
        return "登録しました";
    }

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