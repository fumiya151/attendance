package com.example.attendance.attendance_app.service;

import java.util.Date;
import java.util.Map;
import java.util.List;
import java.util.Optional;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;
import com.example.attendance.attendance_app.model.Employee;
import com.example.attendance.attendance_app.model.EmployeeRole;
import com.example.attendance.attendance_app.repository.LoginRepository;
import com.example.attendance.attendance_app.repository.EmployeeRoleRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LoginService {
    private final LoginRepository loginRepository;
    private final EmployeeRoleRepository employeeRoleRepository;

    private final byte[] jwtSecretBytes = "yourSuperLongSecretKeyForHS512AlgorithmMustBeAtLeast64BytesLong0123456789"
            .getBytes();
    private final long jwtExpirationMs = 3600000;
    private static final String ROLE_DENIED_STATUS = "ROLE_DENIED";

    /**
     * ログイン認証とJWTトークン発行を行うメソッドです.
     *
     * @param employeeId    従業員コード (username)
     * @param password        パスワード
     * @param requestedRole フロントエンドから送られた要求ロール ('user' or 'admin')
     * @return 成功時はMap<"token", String, "employeeId", String>、ロール拒否時はMap<"status",
     *         "ROLE_DENIED">、認証失敗時はnull
     */
    public Map<String, String> loginAndGenerateToken(String employeeId, String password, String requestedRole) {
        Optional<Employee> employeeOptional = loginRepository.findByEmployeeId(employeeId);

        if (employeeOptional.isPresent()) {
            Employee employee = employeeOptional.get();
            String hashedPassword = employee.getPassword();

            if (BCrypt.checkpw(password, hashedPassword)) {

                // --- 認証成功後の処理 ---

                // 【管理者ログインの制限を一時的に解除】
                if ("admin".equals(requestedRole)) {
                    // パスワード認証が通ったため、ロールチェックをスキップして全員許可
                    String token = generateJwtToken(employee);
                    return Map.of("token", token, "employeeId", employee.getEmployeeId());
                }

                // 1. 従業員の実際のロールを取得
                List<EmployeeRole> employeeRoles = employeeRoleRepository.findByEmployeeId(employeeId);

                if (employeeRoles.isEmpty()) {
                    // ロールが割り当てられていない場合はロール拒否と見なす
                    return Map.of("status", ROLE_DENIED_STATUS);
                }

                String actualRoleCode = employeeRoles.get(0).getRole().getRoleCode();

                boolean isPermitted = false;

                // 【打刻ログイン ('user') 制限ロジック】: ADMIN または MGR のみ許可
                if ("user".equals(requestedRole)) {
                    final String[] ADMIN_AND_MGR_ROLES = { "ADMIN", "MGR" };

                    for (String roleCode : ADMIN_AND_MGR_ROLES) {
                        if (roleCode.equals(actualRoleCode)) {
                            isPermitted = true;
                            break;
                        }
                    }
                }

                if (!isPermitted) {
                    // ★ 修正点: ロール制限に引っかかった場合は、特別なMapを返す
                    return Map.of("status", ROLE_DENIED_STATUS);
                }

                // 認証成功 (ロール制限もクリア)
                String token = generateJwtToken(employee);

                return Map.of(
                        "token", token,
                        "employeeId", employee.getEmployeeId());
            }
        }
        // ユーザーIDが見つからない、またはパスワードが不正
        return null;
    }

    /**
     * JWTトークンを生成するメソッドです.
     *
     * @param employee 従業員エンティティ
     * @return JWTトークン文字列
     */
    private String generateJwtToken(Employee employee) {
        // ... (generateJwtTokenメソッドは変更なし) ...
        javax.crypto.SecretKey key = Keys.hmacShaKeyFor(jwtSecretBytes);
        return Jwts.builder()
                .setSubject(employee.getEmployeeId())
                .setIssuedAt(new Date())
                .setExpiration(new Date((new Date()).getTime() + jwtExpirationMs))
                .signWith(key, SignatureAlgorithm.HS512)
                .compact();
    }
}