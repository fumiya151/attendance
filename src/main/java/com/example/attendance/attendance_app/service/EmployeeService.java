package com.example.attendance.attendance_app.service;

import com.example.attendance.attendance_app.dto.EmployeeDto;
import com.example.attendance.attendance_app.model.Employee;
import com.example.attendance.attendance_app.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.security.crypto.bcrypt.BCrypt;

@Service
@RequiredArgsConstructor
public class EmployeeService {
    private final EmployeeRepository employeeRepository;

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
        dto.setId(employee.getId());
        dto.setEmployeeCode(employee.getEmployeeCode());
        dto.setName(employee.getName());
        dto.setDepartment(employee.getDepartment());
        dto.setEmail(employee.getEmail());
        dto.setActive(employee.isActive());
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
     * @param employee 従業員エンティティ
     */
    public void registerEmployee(Employee employee) {
        if (employee.getPassword() != null && !employee.getPassword().isEmpty()) {
            String hashed = BCrypt.hashpw(employee.getPassword(), BCrypt.gensalt());
            employee.setPassword(hashed);
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
     * @param limit   最大表示件数
     * @return 従業員DTOリスト
     */
    public List<EmployeeDto> searchEmployees(String keyword, int limit) {
        List<Employee> employees;
        if (keyword == null || keyword.isEmpty()) {
            employees = employeeRepository.findAll();
        } else {
            employees = employeeRepository.findByNameContainingIgnoreCaseOrEmployeeCodeContainingIgnoreCase(keyword,
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
     * @param id  従業員ID
     * @param dto 更新内容（DTO）
     * @return 更新後の従業員DTO
     */
    public EmployeeDto updateEmployee(Long id, EmployeeDto dto) {
        Employee emp = employeeRepository.findById(id).orElseThrow(() -> new RuntimeException("従業員が見つかりません"));
        emp.setName(dto.getName());
        emp.setDepartment(dto.getDepartment());
        emp.setEmployeeCode(dto.getEmployeeCode());
        emp.setActive(dto.isActive());
        // 必要に応じて他フィールドも更新
        employeeRepository.save(emp);
        return convertToDto(emp);
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
     * @param id 従業員ID
     */
    public void deleteEmployee(Long id) {
        employeeRepository.deleteById(id);
    }
}
