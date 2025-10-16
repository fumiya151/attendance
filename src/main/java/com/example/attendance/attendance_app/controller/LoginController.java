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

    /**
     * ログインAPIのエンドポイントです。
     *
     * 【機能】
     * ユーザー名、パスワード、ロール情報を使用して認証を行い、認証成功時にJWTトークンを生成し返却します。
     *
     * 【注意事項】
     * 認証成功時は200 OKとトークン/IDを、ロール拒否時は403 Forbidden、認証失敗時は401 Unauthorizedを返却します。
     *
     * @param loginRequest ログインリクエスト
     * @return 成功時はJWTトークンと従業員ID、失敗時はエラーメッセージ
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {

        // サービスから認証結果を取得
        Map<String, String> result = loginService.loginAndGenerateToken(
                loginRequest.getUsername(),
                loginRequest.getPassword());

        // 認証成功の判定: resultがnullでない場合
        if (result != null) {
            return ResponseEntity.ok(result);
        }

        // Serviceがnullを返した場合 (認証失敗)
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED) // 401 Unauthorized
                .body(Map.of("message", "ユーザーIDまたはパスワードが正しくありません。"));
    }
}