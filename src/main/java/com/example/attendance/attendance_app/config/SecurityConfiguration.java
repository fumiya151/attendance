package com.example.attendance.attendance_app.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * アプリケーション全体のセキュリティ関連のBeanを定義する設定クラス
 */
@Configuration
public class SecurityConfiguration {

    /**
     * EmployeeService および他の認証サービスが必要とする PasswordEncoder の Bean を定義します。
     *
     * 【機能】
     * パスワードのハッシュ化（暗号化）および検証のために {@code BCryptPasswordEncoder} のインスタンスを提供します。
     *
     * 【注意事項】
     * BCryptは、ストレッチングという手法により計算に時間をかけ、総当たり攻撃に対する耐性を高めています。
     *
     * @return BCryptPasswordEncoder のインスタンス
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        // BCryptPasswordEncoder のインスタンスを Spring コンテナに登録
        return new BCryptPasswordEncoder();
    }
}