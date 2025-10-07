package com.example.attendance.attendance_app.service;

import com.example.attendance.attendance_app.dto.EmployeeDto;
import com.example.attendance.attendance_app.model.Employee;
import com.example.attendance.attendance_app.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder; // ★ 修正：PasswordEncoderを注入

    /**
     * 従業員一覧を取得するメソッドです.
     *
     * 【機能】
     * 従業員情報を全件取得し、DTOリストで返します。
     *
     * 【注意事項】
     * 特になし
     *
     * @return 従業員DTOリスト
     */
    public List<EmployeeDto> getEmployees() {
        return employeeRepository.findAll().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * EmployeeエンティティをDTOに変換するメソッドです.
     *
     * 【機能】
     * エンティティの各項目をDTOにセットします。
     *
     * 【注意事項】
     * 特になし
     *
     * @param employee 従業員エンティティ
     * @return 従業員DTO
     */
    private EmployeeDto convertToDto(Employee employee) {
        EmployeeDto dto = new EmployeeDto();
        dto.setEmployeeId(employee.getEmployeeId());
        dto.setName(employee.getName());
        dto.setDepartment(employee.getDepartment());
        dto.setEmail(employee.getEmail());
        dto.setActive(employee.getIsActive());
        return dto;
    }

    /**
     * 従業員情報を登録するメソッドです.
     *
     * 【機能】
     * 新規従業員情報をDBに保存します。
     *
     * 【注意事項】
     * パスワードはハッシュ化して保存されます。
     *
     * @param employee 従業員エンティティ（employeeIdがセットされていること）
     */
    @Transactional
    public void registerEmployee(Employee employee) {
        // ★ 修正1：主キーの存在チェックと例外処理（IdentifierGenerationExceptionの回避）
        if (employee.getEmployeeId() == null || employee.getEmployeeId().isEmpty()) {
            throw new IllegalArgumentException("従業員ID（主キー）は必須です。");
        }

        if (employee.getPassword() != null && !employee.getPassword().isEmpty()) {
            // ★ 修正2：BCrypt.hashpwの代わりにPasswordEncoderを使用
            String hashedPassword = passwordEncoder.encode(employee.getPassword());
            employee.setPassword(hashedPassword);
        }

        employee.setHireDate(java.time.LocalDate.now());
        employeeRepository.save(employee);
    }

    /**
     * 従業員を氏名またはコードで検索するメソッドです.
     *
     * 【機能】
     * 部分一致検索と表示件数制限が可能です。
     *
     * 【注意事項】
     * キーワードがnullまたは空の場合は全件取得します。
     *
     * @param keyword 検索キーワード（任意）
     * @param limit     最大表示件数
     * @return 従業員DTOリスト
     */
    public List<EmployeeDto> searchEmployees(String keyword, int limit) {
        List<Employee> employees;
        if (keyword == null || keyword.isEmpty()) {
            employees = employeeRepository.findAll();
        } else {
            employees = employeeRepository.findByNameContainingIgnoreCaseOrEmployeeIdContainingIgnoreCase(keyword,
                    keyword);
        }
        return employees.stream()
                .limit(limit)
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * 従業員編集メソッドです.
     *
     * 【機能】
     * 指定IDの従業員情報を更新します。
     *
     * 【注意事項】
     * 特になし
     *
     * @param employeeId 従業員ID
     * @param dto        更新内容（DTO）
     * @return 更新後の従業員DTO
     */
    @Transactional
    public EmployeeDto updateEmployee(String employeeId, EmployeeDto dto) { // ★ 引数名を統一
        Employee emp = employeeRepository.findById(employeeId).orElseThrow(() -> new RuntimeException("従業員が見つかりません"));
        // employeeIdは主キーなので通常は変更しないが、DTOに含まれている場合は更新
        if (!emp.getEmployeeId().equals(dto.getEmployeeId())) {
            throw new IllegalArgumentException("従業員IDは変更できません。");
        }

        emp.setName(dto.getName());
        emp.setDepartment(dto.getDepartment());
        // emp.setEmployeeId(dto.getEmployeeId()); // 主キーの再設定は不要
        emp.setIsActive(dto.isActive());
        emp.setEmail(dto.getEmail());

        // パスワードは更新DTOに含まれないことが多いため、ここでは処理しない

        employeeRepository.save(emp);
        return convertToDto(emp);
    }

    /**
     * 従業員をIDで取得するメソッドです.
     *
     * @param employeeId 従業員ID
     * @return 従業員DTO
     */
    public EmployeeDto getEmployeeById(String employeeId) { // ★ 引数名を統一
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("従業員が見つかりません"));
        return convertToDto(employee);
    }

    /**
     * 従業員削除メソッドです.
     *
     * 【機能】
     * 指定IDの従業員情報を削除します。
     *
     * 【注意事項】
     * 特になし
     *
     * @param employeeId 従業員ID
     */
    @Transactional
    public void deleteEmployee(String employeeId) { // ★ 引数名を統一
        employeeRepository.deleteById(employeeId);
    }
}