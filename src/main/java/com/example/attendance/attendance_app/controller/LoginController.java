package com.example.attendance.attendance_app.controller;

import com.example.attendance.attendance_app.dto.LoginRequest;
import com.example.attendance.attendance_app.service.LoginService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class LoginController {
    private final LoginService loginService;
    private static final String ROLE_DENIED_STATUS = "ROLE_DENIED";

    /**
     * ログインAPIのエンドポイントです.
     *
     * @param loginRequest ログインリクエスト
     * @return 成功時はJWTトークンと従業員ID、失敗時はエラーメッセージ
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {

        // サービスから認証結果を取得
        Map<String, String> result = loginService.loginAndGenerateToken(
                loginRequest.getUsername(),
                loginRequest.getPassword(),
                loginRequest.getRole());

        // 認証成功の判定: tokenとemployeeIdの両方が存在する場合
        if (result != null && result.containsKey("token") && result.containsKey("employeeId")) {
            // トークンと employeeId の両方を返す
            return ResponseEntity.ok(result);
        }

        // 認証失敗の判定

        if (result != null && ROLE_DENIED_STATUS.equals(result.get("status"))) {
            // 権限がない場合 (パスワードは正しいがロール制限で拒否)
            return ResponseEntity.status(HttpStatus.FORBIDDEN) // 403 Forbidden
                    .body(Map.of("message", "アクセス権限がありません。ログインする画面が異なります。"));
        }

        // Serviceがnullを返した場合も含む
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED) // 401 Unauthorized
                .body(Map.of("message", "ユーザーIDまたはパスワードが正しくありません。"));
    }
}