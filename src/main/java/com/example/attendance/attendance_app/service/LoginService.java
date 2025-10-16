package com.example.attendance.attendance_app.service;

import com.example.attendance.attendance_app.model.Employee;
import com.example.attendance.attendance_app.model.EmployeeRole;
import com.example.attendance.attendance_app.repository.EmployeeRoleRepository;
import com.example.attendance.attendance_app.repository.LoginRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LoginService {
    private final LoginRepository loginRepository;
    private final EmployeeRoleRepository employeeRoleRepository;
    private final JwtService jwtService; // Inject JwtService
    private final PasswordEncoder passwordEncoder; // Inject PasswordEncoder

    /**
     * 従業員IDとパスワードで認証し、成功時にJWTトークンを発行します。
     *
     * @param employeeId 従業員コード (username)
     * @param password   パスワード
     * @return 成功時はMap<"token", String, "employeeId", String, "actualRole", String>、
     *         認証失敗時はnull
     */
    public Map<String, String> loginAndGenerateToken(String employeeId, String password) {
        Optional<Employee> employeeOptional = loginRepository.findByEmployeeId(employeeId);

        if (employeeOptional.isPresent()) {
            Employee employee = employeeOptional.get();
            // BCryptPasswordEncoderを使用してパスワードを検証
            if (passwordEncoder.matches(password, employee.getPassword())) {

                // 従業員のロールを取得
                List<EmployeeRole> employeeRoles = employeeRoleRepository.findByEmployeeId(employeeId);
                String actualRoleCode = "USER"; // デフォルトロール
                if (!employeeRoles.isEmpty()) {
                    // 最初のロールを返す (アプリケーションのロジックに合わせて調整が必要な場合があります)
                    actualRoleCode = employeeRoles.get(0).getRole().getRoleCode();
                }

                // JwtServiceを使用してトークンを生成
                String token = jwtService.generateToken(employee);

                return Map.of(
                        "token", token,
                        "employeeId", employee.getEmployeeId(),
                        "actualRole", actualRoleCode);
            }
        }
        // ユーザーIDが見つからない、またはパスワードが不正
        return null;
    }
}