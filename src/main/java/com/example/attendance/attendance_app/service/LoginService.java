package com.example.attendance.attendance_app.service;

import java.util.Date;
import java.util.Optional;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;
import com.example.attendance.attendance_app.model.Employee;
import com.example.attendance.attendance_app.repository.LoginRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LoginService {
    private final LoginRepository loginRepository;

    // HS512用の安全なキー（512bit以上）
    private final byte[] jwtSecretBytes = "yourSuperLongSecretKeyForHS512AlgorithmMustBeAtLeast64BytesLong0123456789"
            .getBytes();
    private final long jwtExpirationMs = 3600000; // 1時間

    /**
     * ログイン認証とJWTトークン発行を行うメソッドです.
     *
     * 【機能】
     * 認証成功時にJWTトークンを返します。
     *
     * 【注意事項】
     * パスワードはハッシュ化されている必要があります。
     *
     * @param employeeId 従業員コード
     * @param password   パスワード
     * @return JWTトークン（認証失敗時はnull）
     */
    public String loginAndGenerateToken(String employeeId, String password) {
        Optional<Employee> employeeOptional = loginRepository.findByEmployeeId(employeeId);
        if (employeeOptional.isPresent()) {
            Employee employee = employeeOptional.get();
            String hashedPassword = employee.getPassword();
            if (BCrypt.checkpw(password, hashedPassword)) {
                return generateJwtToken(employee);
            }
        }
        return null;
    }

    /**
     * JWTトークンを生成するメソッドです.
     *
     * 【機能】
     * 従業員情報からJWTトークンを生成します。
     *
     * 【注意事項】
     * HS512用の安全なキーが必要です。
     *
     * @param employee 従業員エンティティ
     * @return JWTトークン文字列
     */
    private String generateJwtToken(Employee employee) {
        // HS512用の安全なキー生成
        javax.crypto.SecretKey key = Keys.hmacShaKeyFor(jwtSecretBytes);
        return Jwts.builder()
                .setSubject(employee.getEmployeeId())
                .setIssuedAt(new Date())
                .setExpiration(new Date((new Date()).getTime() + jwtExpirationMs))
                .signWith(key, SignatureAlgorithm.HS512)
                .compact();
    }
}
