package com.example.attendance.attendance_app.dto;

import lombok.Data;

/**
 * 新規従業員登録リクエストの情報を保持するDTOです。
 *
 * 【用途】
 * 従業員マスタ登録API (/api/employees) を通じて、新しい従業員の基本情報と認証情報を登録するために利用されます。
 */
@Data
public class EmployeeRegistrationRequest {
    /**
     * 必須：従業員コード（ログインIDとして使用）。
     */
    private String employeeId;

    /**
     * 必須：氏名。
     */
    private String name;

    /**
     * 必須：所属部署または役職。
     */
    private String department;

    /**
     * 必須：メールアドレス。
     */
    private String email;

    /**
     * 必須：初期パスワード（ハッシュ化して保存されます）。
     */
    private String password;

    /**
     * 必須：割り当てる役割のID（ロールID）。
     */
    private Long roleId;

    /**
     * 必須：時給（賃金履歴の初期値として使用されます）。
     */
    private String wage;
}