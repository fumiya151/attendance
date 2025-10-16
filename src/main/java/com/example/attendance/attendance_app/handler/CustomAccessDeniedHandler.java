package com.example.attendance.attendance_app.handler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collection;

@Component
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException) throws IOException, ServletException {

        // ログイン中のユーザー情報を取得
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        // ユーザーが認証済みであるか、かつ権限情報を持っているか確認
        if (auth != null) {
            Collection<? extends GrantedAuthority> authorities = auth.getAuthorities();
            boolean isEmp = authorities.stream()
                    .map(GrantedAuthority::getAuthority)
                    .anyMatch("EMP"::equals); // ユーザーが'EMP'ロールを持っているか確認

            // 権限がEMPの場合（URL直打ちなどで管理者ページにアクセスしようとした場合を想定）
            if (isEmp) {
                response.sendRedirect("/html/emp_main.html");
                return;
            }
        }

        // それ以外の場合（未認証ユーザー、またはその他の権限不足ユーザー）は、デフォルトの403エラーを返す
        response.sendError(HttpServletResponse.SC_FORBIDDEN, "Access Denied"); // 403 Forbidden
    }
}