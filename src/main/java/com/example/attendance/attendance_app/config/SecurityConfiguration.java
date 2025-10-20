package com.example.attendance.attendance_app.config;

import com.example.attendance.attendance_app.filter.JwtAuthFilter;
import com.example.attendance.attendance_app.handler.CustomAccessDeniedHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Securityの主要な設定クラスです。
 *
 * 【機能】
 * アプリケーション全体のセキュリティポリシーを定義します。
 * 主にJWTによるステートレス認証、CSRF無効化、リソースごとのアクセス認可を設定します。
 *
 * 【注意事項】
 * セッションは使用せず（STATELESS）、全ての認証済みリクエストはJWTトークンを必要とします。
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfiguration {

    private final JwtAuthFilter jwtAuthFilter;
    private final CustomAccessDeniedHandler customAccessDeniedHandler;

    /**
     * アプリケーション全体で使用されるパスワードエンコーダーのBeanを定義します。
     *
     * 【機能】
     * パスワードのハッシュ化と検証のためにBCryptPasswordEncoderを提供します。
     *
     * @return BCryptPasswordEncoderのインスタンス
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * セキュリティフィルターチェーンを構築します。
     *
     * 【機能】
     * 以下のルールに従い、HTTPリクエストの認可を設定し、カスタムフィルターを適用します。
     * 1. ログイン、静的リソース、H2コンソール、全てのHTMLファイルへのアクセスは認証不要（permitAll）。
     * 2. 管理者API（/api/employees, /api/payrollなど）は「ROLE_ADMIN」または「ROLE_MGR」に限定。
     * 3. 勤怠API（/api/summaries, /api/attendanceなど）は全ての認証済ユーザーに許可（authenticated）。
     *
     * 【注意事項】
     * 認証（JWT）フィルターが認証処理の前に実行され、認証失敗時や権限不足時はカスタムハンドラーが応答を処理します。
     *
     * @param http HttpSecurityオブジェクト
     * @return 構築されたSecurityFilterChain
     * @throws Exception 設定エラー時
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authz -> authz
                        // --- 1. 認証不要 (Permit All) ---
                        // ユーザーがブラウザで直接アクセスする全てのHTML、CSS、JSはpermitAllにする
                        .requestMatchers(
                                "/",
                                "/h2-console/**",
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/api/auth/login",
                                "/css/**",
                                "/js/**",
                                "/html/admin_login.html",
                                "/html/punch_login.html",
                                "/error",
                                "/html/admin_main.html",
                                "/html/emp_main.html",
                                "/html/main.html",
                                "/html/employee_management.html",
                                "/html/register_employee.html",
                                "/html/edit_employee.html",
                                "/html/edit_employee_confirm.html",
                                "/html/payroll_calculation.html",
                                "/html/system_settings.html",
                                "/html/edit_attendance.html",
                                "/html/attendance_log.html",
                                "/html/emp_system_settings.html",
                                "/html/emp_edit_attendance.html",
                                "/api/employees", // 従業員名簿の全件取得API
                                                  // (admin_mainで使うが、データ制限はサービス層で行う前提でpermitAll)
                                "/api/attendance/next-available/**", // 打刻ボタン制御API
                                "/api/attendance/stamp") // 打刻API
                        .permitAll()

                        // --- 2. 管理者限定 (ROLE_ADMIN, ROLE_MGR) ---
                        // 管理者専用のAPIのみをロールで保護
                        .requestMatchers(
                                "/admin/**",
                                "/api/employees/**", // 従業員マスタAPI（GETを除くCRUD操作など）
                                "/api/payroll/**") // 給与計算API
                        .hasAnyAuthority("ROLE_ADMIN", "ROLE_MGR")

                        // --- 3. 全ての認証ユーザーに許可 (Authenticated) ---
                        // 勤怠関連のAPIのみを認証で保護
                        .requestMatchers(
                                "/api/summaries/**",
                                "/api/attendance/**")
                        .authenticated()

                        // --- 4. その他 ---
                        // 上記に該当しない他の全てのリクエストは認証を必要とする
                        .anyRequest().authenticated())
                .exceptionHandling(exceptions -> exceptions
                        // 権限不足時のカスタムハンドラーのみを登録
                        .accessDeniedHandler(customAccessDeniedHandler))
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        // H2 ConsoleがframeOptionsを使用できるように設定（開発環境向け）
        http.headers(headers -> headers.frameOptions(frameOptions -> frameOptions.sameOrigin()));

        return http.build();
    }
}