package com.example.attendance.attendance_app.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import com.example.attendance.attendance_app.dto.EmployeeDto;
import com.example.attendance.attendance_app.dto.EmployeeRegistrationRequest;
import com.example.attendance.attendance_app.service.EmployeeService;

import java.util.List;

@RestController
@RequestMapping("/api/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;

    // ★ 定数としてヘッダー名を定義しておくと便利です
    private static final String OPERATOR_HEADER = "X-Operator-Id";

    /**
     * 従業員登録APIのエンドポイントです.
     *
     * @param request    従業員登録リクエスト (EmployeeRegistrationRequest DTO)
     * @param operatorId 登録操作を行った従業員ID (ヘッダーから取得)
     * @return 登録結果メッセージ
     */
    @PostMapping
    public String registerEmployee(
            @RequestBody EmployeeRegistrationRequest request,
            @RequestHeader(OPERATOR_HEADER) String operatorId) { // ★ 修正点: ヘッダーから操作者IDを取得

        // ★ 修正点: Serviceに操作者IDを渡す
        employeeService.registerEmployee(request, operatorId);
        return "登録しました";
    }

    /**
     * 従業員一覧取得APIのエンドポイントです.
     *
     * 【機能】
     * 従業員情報を全件取得し返却します。
     *
     * @return 従業員DTOリスト
     */
    @GetMapping
    public List<EmployeeDto> getEmployees(
            @RequestParam(required = false) String keyword) {
        return employeeService.searchEmployees(keyword, Integer.MAX_VALUE);
    }

    /**
     * 従業員一覧取得API（3件制限）
     *
     * @return 従業員DTOリスト
     */
    @GetMapping("/limited")
    public List<EmployeeDto> getEmployeesLimited(
            @RequestParam(required = false) String keyword) {
        return employeeService.searchEmployees(keyword, 3);
    }

    /**
     * 従業員をIDで取得するAPIのエンドポイントです.
     *
     * @param employeeId 従業員ID
     * @return 従業員DTO
     */
    @GetMapping("/{id}")
    public EmployeeDto getEmployeeById(@PathVariable("id") String employeeId) {
        return employeeService.getEmployeeById(employeeId);
    }

    /**
     * 従業員編集APIのエンドポイントです.
     *
     * @param employeeId 従業員ID
     * @param dto                      更新内容（DTO）
     * @param updaterId  更新操作を行った従業員ID (ヘッダーから取得)
     * @return 更新後の従業員DTO
     */
    @PutMapping("/{id}")
    public EmployeeDto updateEmployee(
            @PathVariable("id") String employeeId,
            @RequestBody EmployeeDto dto,
            @RequestHeader(OPERATOR_HEADER) String updaterId) { // ★ 修正点: ヘッダーから更新者IDを取得

        // ★ 修正点: Serviceに更新者IDを渡す
        return employeeService.updateEmployee(employeeId, dto, updaterId);
    }

    /**
     * 従業員削除APIのエンドポイントです.
     *
     * @param employeeId 従業員ID
     */
    @DeleteMapping("/{id}")
    public void deleteEmployee(@PathVariable("id") String employeeId) {
        employeeService.deleteEmployee(employeeId);
    }
}