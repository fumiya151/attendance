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

    /**
     * 従業員登録APIのエンドポイントです.
     *
     * 【機能】
     * 新規従業員情報と役割を登録します。
     *
     * 【注意事項】
     * パスワードはハッシュ化されて保存されます。
     *
     * @param request 従業員登録リクエスト (EmployeeRegistrationRequest DTO)
     * @return 登録結果メッセージ
     */
    @PostMapping
    public String registerEmployee(@RequestBody EmployeeRegistrationRequest request) { // 変更点: DTOを使用
        employeeService.registerEmployee(request); // 変更点: DTOをServiceに渡す
        return "登録しました";
    }

    /**
     * 従業員一覧取得APIのエンドポイントです.
     *
     * 【機能】
     * 従業員情報を全件取得し返却します。
     *
     * 【注意事項】
     * 特になし
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
     * 【機能】
     * 従業員情報を最大3件まで取得し返却します。
     *
     * 【注意事項】
     * 特になし
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
    public EmployeeDto getEmployeeById(@PathVariable("id") String employeeId) { // ★ 修正: 明示的に "id" を指定
        return employeeService.getEmployeeById(employeeId);
    }

    /**
     * 従業員編集APIのエンドポイントです.
     *
     * 【機能】
     * 指定IDの従業員情報を更新します。
     *
     * 【注意事項】
     * 特になし
     *
     * @param employeeId 従業員ID
     * @param dto               更新内容（DTO）
     * @return 更新後の従業員DTO
     */
    @PutMapping("/{id}")
    public EmployeeDto updateEmployee(@PathVariable("id") String employeeId, @RequestBody EmployeeDto dto) { // ★ 修正:
        // 明示的に
        // "id" を指定
        return employeeService.updateEmployee(employeeId, dto);
    }

    /**
     * 従業員削除APIのエンドポイントです.
     *
     * 【機能】
     * 指定IDの従業員情報を削除します。
     *
     * 【注意事項】
     * 関連データがある場合は削除時エラーになる可能性があります。
     *
     * @param employeeId 従業員ID
     */
    @DeleteMapping("/{id}")
    public void deleteEmployee(@PathVariable("id") String employeeId) { // ★ 修正: 明示的に "id" を指定
        employeeService.deleteEmployee(employeeId);
    }
}
