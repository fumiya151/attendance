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

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfiguration {

    private final JwtAuthFilter jwtAuthFilter;
    private final CustomAccessDeniedHandler customAccessDeniedHandler;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authz -> authz
                        // --- 1. 認証不要 (Permit All) ---
                        // ユーザーがブラウザで直接アクセスするすべてのHTML、CSS、JSはpermitAllにする
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
                                "/api/employees",
                                "/api/attendance/next-available/**",
                                "/api/attendance/stamp")
                        .permitAll()

                        // --- 2. 管理者限定 (ROLE_ADMIN, ROLE_MGR) ---
                        // 管理者専用のAPIのみをロールで保護
                        .requestMatchers(
                                "/admin/**",
                                "/api/employees/**", // API
                                "/api/payroll/**") // API
                        .hasAnyAuthority("ROLE_ADMIN", "ROLE_MGR")

                        // --- 3. 全ての認証ユーザーに許可 (Authenticated) ---
                        // 勤怠関連のAPIのみを認証で保護
                        .requestMatchers(
                                "/api/summaries/**",
                                "/api/attendance/**")
                        .authenticated()

                        // --- 4. その他 ---
                        .anyRequest().authenticated())
                .exceptionHandling(exceptions -> exceptions
                        // 権限不足時のカスタムハンドラーのみを登録
                        .accessDeniedHandler(customAccessDeniedHandler))
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        http.headers(headers -> headers.frameOptions(frameOptions -> frameOptions.sameOrigin()));

        return http.build();
    }
}