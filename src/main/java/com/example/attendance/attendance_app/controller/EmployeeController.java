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
    private static final String OPERATOR_HEADER = "X-Operator-Id";

    /**
     * 従業員登録APIのエンドポイントです。
     *
     * 【機能】
     * 渡されたリクエストデータに基づき、新しい従業員をシステムに登録します。
     *
     * 【注意事項】
     * 登録操作を行ったオペレーターのIDがヘッダーから取得され、監査ログなどに利用されます。
     *
     * @param request    従業員登録リクエスト (EmployeeRegistrationRequest DTO)
     * @param operatorId 登録操作を行った従業員ID (ヘッダーから取得)
     * @return 登録結果メッセージ
     */
    @PostMapping
    public String registerEmployee(
            @RequestBody EmployeeRegistrationRequest request,
            @RequestHeader(OPERATOR_HEADER) String operatorId) {

        employeeService.registerEmployee(request, operatorId);
        return "登録しました";
    }

    /**
     * 従業員一覧取得APIのエンドポイントです。
     *
     * 【機能】
     * 従業員情報を全件取得し返却します。キーワードが指定された場合は部分一致で絞り込みます。
     *
     * 【注意事項】
     * パラメータの制限がなく、全件取得するため、大量のデータが存在する場合は性能に注意が必要です。
     *
     * @param keyword 検索キーワード（オプション）
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
     * 従業員情報を取得し、結果を最大3件に制限して返却します。キーワードによる絞り込みも可能です。
     *
     * 【注意事項】
     * 主に画面のサジェストやダッシュボードでの限定的な表示に利用されます。
     *
     * @param keyword 検索キーワード（オプション）
     * @return 従業員DTOリスト
     */
    @GetMapping("/limited")
    public List<EmployeeDto> getEmployeesLimited(
            @RequestParam(required = false) String keyword) {
        return employeeService.searchEmployees(keyword, 3);
    }

    /**
     * 従業員をIDで取得するAPIのエンドポイントです。
     *
     * 【機能】
     * パス変数で指定された従業員IDに一致する従業員情報を取得し返却します。
     *
     * 【注意事項】
     * 該当する従業員が存在しない場合、Service層から例外がスローされる場合があります。
     *
     * @param employeeId 従業員ID
     * @return 従業員DTO
     */
    @GetMapping("/{id}")
    public EmployeeDto getEmployeeById(@PathVariable("id") String employeeId) {
        return employeeService.getEmployeeById(employeeId);
    }

    /**
     * 従業員編集APIのエンドポイントです。
     *
     * 【機能】
     * 指定された従業員IDの情報を、リクエストボディのDTOの内容で更新します。
     *
     * 【注意事項】
     * 更新操作を行ったオペレーターのIDがヘッダーから取得され、更新日時とともに記録されます。
     *
     * @param employeeId 従業員ID
     * @param dto        更新内容（DTO）
     * @param updaterId  更新操作を行った従業員ID (ヘッダーから取得)
     * @return 更新後の従業員DTO
     */
    @PutMapping("/{id}")
    public EmployeeDto updateEmployee(
            @PathVariable("id") String employeeId,
            @RequestBody EmployeeDto dto,
            @RequestHeader(OPERATOR_HEADER) String updaterId) {

        return employeeService.updateEmployee(employeeId, dto, updaterId);
    }

    /**
     * 従業員削除APIのエンドポイントです。
     *
     * 【機能】
     * 指定IDの従業員情報を削除します。操作者IDをServiceに渡し、監査ロジックに対応します。
     *
     * 【注意事項】
     * 物理削除ではなく、論理削除として実装されている場合があります。
     *
     * @param employeeId 従業員ID
     * @param operatorId 削除操作を行った従業員ID (ヘッダーから取得)
     */
    @DeleteMapping("/{id}")
    public void deleteEmployee(
            @PathVariable("id") String employeeId,
            @RequestHeader(OPERATOR_HEADER) String operatorId) {
        employeeService.deleteEmployee(employeeId, operatorId);
    }
}