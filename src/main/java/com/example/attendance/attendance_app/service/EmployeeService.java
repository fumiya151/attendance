package com.example.attendance.attendance_app.service;

import com.example.attendance.attendance_app.dto.EmployeeDto;
import com.example.attendance.attendance_app.dto.EmployeeRegistrationRequest;
import com.example.attendance.attendance_app.model.Employee;
import com.example.attendance.attendance_app.model.EmployeeRole;
import com.example.attendance.attendance_app.model.EmployeeTaxInfo;
import com.example.attendance.attendance_app.model.EmployeeWageHistory;
import com.example.attendance.attendance_app.model.Role;
import com.example.attendance.attendance_app.repository.EmployeeRepository;
import com.example.attendance.attendance_app.repository.EmployeeRoleRepository;
import com.example.attendance.attendance_app.repository.EmployeeTaxInfoRepository;
import com.example.attendance.attendance_app.repository.EmployeeWageHistoryRepository;
import com.example.attendance.attendance_app.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import java.math.BigDecimal;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final EmployeeRoleRepository employeeRoleRepository;
    private final RoleRepository roleRepository;
    private final EmployeeWageHistoryRepository wageHistoryRepository;
    private final EmployeeTaxInfoRepository employeeTaxInfoRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * 新規従業員にデフォルトの役割を割り当てるプライベートメソッドです。
     *
     * 【機能】
     * 従業員IDと役割IDに基づき、EmployeeRoleテーブルにエントリを作成します。
     *
     * @param employee     役割を割り当てる従業員エンティティ
     * @param roleId         割り当てる役割のID
     * @param operatorId 操作を行った従業員ID
     */
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
     * 有効な（アクティブな）従業員情報を全件取得し、DTOリストで返します。
     *
     * 【注意事項】
     * 論理削除された従業員（isActive=false）は除外されます。
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
     * エンティティの各項目をDTOにセットします。時給および税務情報を取得しDTOにセットします。
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

        // 1. 現在有効な時給を取得してDTOにセット
        wageHistoryRepository.findApplicableWageByEmployeeIdAndDate(employee.getEmployeeId(), LocalDate.now())
                .ifPresent(wageHistory -> {
                    dto.setWage(wageHistory.getHourlyWage().toString());
                });

        // 2. 税務情報を取得してDTOにセット
        employeeTaxInfoRepository.findByEmployeeId(employee.getEmployeeId())
                .ifPresent(taxInfo -> {
                    dto.setDependentCount(taxInfo.getDependentCount());
                    dto.setMonthlyResidentTax(taxInfo.getMonthlyResidentTax().doubleValue());
                });

        return dto;
    }

    /**
     * 従業員情報を登録するメソッドです.
     *
     * 【機能】
     * リクエストDTOの内容に基づき、Employeeテーブルに新規従業員を登録し、パスワードをハッシュ化します。
     * 登録後、指定された役割IDに基づきデフォルトの役割を割り当てます。また、税務情報テーブルにデフォルトエントリを追加します。
     *
     * @param request       従業員登録リクエストDTO
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

        // ★修正: 新規登録時は、TaxInfo を直接 INSERT するロジックを使用★
        EmployeeTaxInfo defaultTaxInfo = new EmployeeTaxInfo();
        defaultTaxInfo.setEmployeeId(savedEmployee.getEmployeeId());
        defaultTaxInfo.setDependentCount(0); // 扶養人数0をデフォルト
        defaultTaxInfo.setMonthlyResidentTax(BigDecimal.ZERO); // 住民税0円をデフォルト
        defaultTaxInfo.setEffectiveDate(LocalDate.now());
        employeeTaxInfoRepository.save(defaultTaxInfo);
    }

    /**
     * 従業員を氏名またはコードで検索するメソッドです.
     *
     * 【機能】
     * キーワード（氏名または従業員ID）に基づき、アクティブな従業員を部分一致検索し、最大表示件数で制限します。
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
     * 指定IDの従業員情報を更新します。氏名、役職、メールアドレス、役割、時給、および税務情報の変更を処理します。
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

        // 時給の更新ロジック
        if (dto.getWage() != null && !dto.getWage().isEmpty()) {
            BigDecimal newWage = new BigDecimal(dto.getWage());
            Optional<EmployeeWageHistory> currentWageOpt = wageHistoryRepository
                    .findApplicableWageByEmployeeIdAndDate(employeeId, LocalDate.now());

            if (currentWageOpt.isPresent()) {
                EmployeeWageHistory currentWage = currentWageOpt.get();
                if (currentWage.getHourlyWage().compareTo(newWage) != 0) {
                    // 現在の時給レコードを終了させる
                    currentWage.setEffectiveEndDate(LocalDate.now().minusDays(1));
                    wageHistoryRepository.save(currentWage);

                    // 新しい時給レコードを作成
                    EmployeeWageHistory newWageHistory = new EmployeeWageHistory();
                    newWageHistory.setEmployee(emp);
                    newWageHistory.setHourlyWage(newWage);
                    newWageHistory.setEffectiveStartDate(LocalDate.now());
                    wageHistoryRepository.save(newWageHistory);
                }
            } else {
                // 時給レコードがまだない場合、新規作成
                EmployeeWageHistory newWageHistory = new EmployeeWageHistory();
                newWageHistory.setEmployee(emp);
                newWageHistory.setHourlyWage(newWage);
                newWageHistory.setEffectiveStartDate(LocalDate.now());
                wageHistoryRepository.save(newWageHistory);
            }
        }

        // ★追加: 税務情報の更新ロジック★
        if (dto.getDependentCount() != null || dto.getMonthlyResidentTax() != null) {
            // DTOから扶養人数と住民税額を取得
            int dependentCount = dto.getDependentCount() != null ? dto.getDependentCount() : 0;
            double residentTax = dto.getMonthlyResidentTax() != null ? dto.getMonthlyResidentTax() : 0.0;

            // 税務情報を保存/更新
            saveOrUpdateTaxInfo(employeeId, dependentCount, residentTax, LocalDate.now());
        }

        employeeRepository.save(emp);
        return convertToDto(emp);
    }

    /**
     * 従業員をIDで取得するメソッドです.
     *
     * 【機能】
     * 指定された従業員IDに対応する従業員情報を取得し、DTOとして返します。
     *
     * @param employeeId 従業員ID
     * @return 従業員DTO
     * @throws RuntimeException 従業員が見つからない場合
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
     * 指定IDの従業員情報のisActiveフラグをfalseに設定し、論理的に退職処理を行います。
     *
     * @param employeeId 従業員ID
     * @param operatorId 削除操作を行った従業員ID
     * @throws RuntimeException 従業員が見つからない場合
     */
    @Transactional
    public void deleteEmployee(String employeeId, String operatorId) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("従業員が見つかりません"));

        employee.setIsActive(false);
        employeeRepository.save(employee);
    }

    // ----------------------------------------------------
    // ヘルパーメソッド for EmployeeTaxInfoの保存/更新
    // ----------------------------------------------------
    /**
     * 従業員の税務情報（扶養人数、住民税月額）を保存または更新します。
     *
     * 【機能】
     * EmployeeTaxInfoテーブルのレコードが存在しない場合は新規作成し、存在する場合は更新します。
     *
     * @param employeeId         従業員ID
     * @param dependentCount     扶養人数
     * @param monthlyResidentTax 月額住民税
     * @param effectiveDate      設定の適用開始日
     */
    @Transactional
    private void saveOrUpdateTaxInfo(String employeeId, int dependentCount, double monthlyResidentTax,
            LocalDate effectiveDate) {
        // findById() は Optional を返すため、orElseGet() で新規エンティティを生成
        EmployeeTaxInfo taxInfo = employeeTaxInfoRepository.findById(employeeId)
                .orElseGet(EmployeeTaxInfo::new);

        // 新規作成の場合はIDを設定
        if (taxInfo.getEmployeeId() == null) {
            taxInfo.setEmployeeId(employeeId);
        }

        // 既存の値と異なる場合にのみ更新を行う（簡易化のため、ここでは常に更新）
        taxInfo.setDependentCount(dependentCount);
        taxInfo.setMonthlyResidentTax(BigDecimal.valueOf(monthlyResidentTax));
        taxInfo.setEffectiveDate(effectiveDate);

        employeeTaxInfoRepository.save(taxInfo);
    }
}