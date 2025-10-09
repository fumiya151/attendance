package com.example.attendance.attendance_app.service;

import com.example.attendance.attendance_app.dto.EmployeeDto;
import com.example.attendance.attendance_app.dto.EmployeeRegistrationRequest;
import com.example.attendance.attendance_app.model.Employee;
import com.example.attendance.attendance_app.model.EmployeeRole;
import com.example.attendance.attendance_app.model.Role;
import com.example.attendance.attendance_app.repository.EmployeeRepository;
import com.example.attendance.attendance_app.repository.EmployeeRoleRepository;
import com.example.attendance.attendance_app.repository.RoleRepository;
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
    private final EmployeeRoleRepository employeeRoleRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    private void assignDefaultRole(Employee employee, Long roleId, String operatorId) {
        if (roleId == null) {
            throw new IllegalArgumentException("役割IDは必須です。");
        }

        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new RuntimeException("指定された役割IDが見つかりません: " + roleId));

        EmployeeRole employeeRole = new EmployeeRole();
        employeeRole.setEmployeeId(employee.getEmployeeId());
        employeeRole.setRole(role);
        employeeRole.setUpdatedByEmployeeId(operatorId);
        employeeRoleRepository.save(employeeRole);
    }

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
                .filter(Employee::getIsActive)
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * EmployeeエンティティをDTOに変換するメソッドです.
     *
     * 【機能】
     * エンティティの各項目をDTOにセットし、**employeeテーブルのdepartment（役職名）**をDTOにセットします。（元の動作に戻す）
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
        dto.setEmail(employee.getEmail());
        dto.setActive(employee.getIsActive());
        dto.setDepartment(employee.getDepartment());
        return dto;
    }

    /**
     * 従業員情報を登録するメソッドです.
     *
     * @param request    従業員登録リクエストDTO
     * @param operatorId 登録操作を行った従業員ID
     */
    @Transactional
    public void registerEmployee(EmployeeRegistrationRequest request, String operatorId) {
        if (request.getEmployeeId() == null || request.getEmployeeId().isEmpty()) {
            throw new IllegalArgumentException("従業員ID（主キー）は必須です。");
        }

        Employee employee = new Employee();
        employee.setEmployeeId(request.getEmployeeId());
        employee.setName(request.getName());
        employee.setDepartment(request.getDepartment());
        employee.setEmail(request.getEmail());
        employee.setIsActive(true);

        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            String hashedPassword = passwordEncoder.encode(request.getPassword());
            employee.setPassword(hashedPassword);
        } else {
            throw new IllegalArgumentException("パスワードは必須です。");
        }

        employee.setHireDate(java.time.LocalDate.now());

        Employee savedEmployee = employeeRepository.save(employee);

        assignDefaultRole(savedEmployee, request.getRoleId(), operatorId);
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
                .filter(Employee::getIsActive)
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
     * @param dto               更新内容（DTO）
     * @param updaterId   更新操作を行った従業員ID
     * @return 更新後の従業員DTO
     */
    @Transactional
    public EmployeeDto updateEmployee(String employeeId, EmployeeDto dto, String updaterId) {
        Employee emp = employeeRepository.findById(employeeId).orElseThrow(() -> new RuntimeException("従業員が見つかりません"));

        if (dto.getEmployeeId() != null && !emp.getEmployeeId().equals(dto.getEmployeeId())) {
            throw new IllegalArgumentException("従業員IDは変更できません。");
        }

        emp.setName(dto.getName());
        emp.setDepartment(dto.getDepartment());
        emp.setEmail(dto.getEmail());

        // Role update logic
        if (dto.getRoleId() != null) {
            Role newRole = roleRepository.findById(dto.getRoleId())
                    .orElseThrow(() -> new RuntimeException("指定された役割IDが見つかりません: " + dto.getRoleId()));

            List<EmployeeRole> employeeRoles = employeeRoleRepository.findByEmployeeId(employeeId);
            if (employeeRoles.isEmpty()) {
                EmployeeRole newEmployeeRole = new EmployeeRole();
                newEmployeeRole.setEmployeeId(employeeId);
                newEmployeeRole.setRole(newRole);
                newEmployeeRole.setUpdatedByEmployeeId(updaterId);
                employeeRoleRepository.save(newEmployeeRole);
            } else {
                EmployeeRole employeeRole = employeeRoles.get(0);
                employeeRole.setRole(newRole);
                employeeRole.setUpdatedByEmployeeId(updaterId);
                employeeRoleRepository.save(employeeRole);
            }
        }

        employeeRepository.save(emp);
        return convertToDto(emp);
    }

    /**
     * 従業員をIDで取得するメソッドです.
     *
     * @param employeeId 従業員ID
     * @return 従業員DTO
     */
    public EmployeeDto getEmployeeById(String employeeId) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("従業員が見つかりません"));
        return convertToDto(employee);
    }

    /**
     * 従業員削除メソッドです (論理削除).
     *
     * 【機能】
     * 指定IDの従業員情報を削除します。
     *
     * 【注意事項】
     * 特になし
     *
     * @param employeeId 従業員ID
     * @param operatorId 削除操作を行った従業員ID
     */
    @Transactional
    public void deleteEmployee(String employeeId, String operatorId) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("従業員が見つかりません"));

        employee.setIsActive(false);
        employeeRepository.save(employee);
    }
}