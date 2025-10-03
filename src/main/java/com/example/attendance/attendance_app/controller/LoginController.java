package com.example.attendance.attendance_app.controller;

import com.example.attendance.attendance_app.dto.LoginRequest;
import com.example.attendance.attendance_app.service.EmployeeService;
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

    private final EmployeeService employeeService;

        /**
         * ログインAPIのエンドポイントです.
         *
         * 【機能】
         * ユーザー認証を行い、JWTトークンを返却します。
         *
         *【注意事項】
         * 認証失敗時は401を返します。
         *
         * @param loginRequest ログインリクエスト
         * @return JWTトークン or エラーメッセージ
         */
        @PostMapping("/login")
        public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
            String token = employeeService.loginAndGenerateToken(loginRequest.getUsername(), loginRequest.getPassword());
            if (token != null) {
                return ResponseEntity.ok(Map.of("token", token));
            }
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "ユーザーIDまたはパスワードが正しくありません。"));
    }
}
