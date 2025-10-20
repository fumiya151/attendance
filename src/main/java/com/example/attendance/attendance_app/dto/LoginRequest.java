package com.example.attendance.attendance_app.dto;

import lombok.Data;

/**
 * ログインリクエストで送信される認証情報を保持するDTOです。
 *
 * 【用途】
 * /api/auth/login エンドポイントを通じて、ユーザー認証を行うために利用されます。
 */
@Data
public class LoginRequest {
    /**
     * ユーザー名（従業員ID）。
     */
    private String username;

    /**
     * パスワード。
     */
    private String password;

    /**
     * ログインを要求する役割（ロール）。
     * （例: "user" / "admin" / "MANAGER" など。ロールベースのアクセス制御に使用されます。）
     */
    private String role;
}