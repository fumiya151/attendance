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

/**
 * アクセスが拒否された際（認証済みだが権限不足の場合）にカスタム処理を実行するハンドラーです。
 *
 * 【機能】
 * 認証済みのユーザーが権限を持たないリソース（例：管理API、管理者画面）にアクセスを試みた場合、
 * ユーザーのロールに基づいた応答を返します。
 *
 * 【注意事項】
 * ロールが 'EMP' の場合、強制的に従業員専用のメイン画面（/html/emp_main.html）へリダイレクトします。
 * それ以外の権限不足の場合は、HTTP 403 Forbiddenエラーを返します。
 */
@Component
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    /**
     * アクセス拒否時のハンドリング処理です。
     *
     * @param request               HTTPリクエスト
     * @param response              HTTPレスポンス
     * @param accessDeniedException 発生したAccessDeniedException
     * @throws IOException      リダイレクトやエラー応答時のI/Oエラー
     * @throws ServletException サーブレット処理中のエラー
     */
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

            // ユーザーが'EMP'ロールを持っているか確認
            boolean isEmp = authorities.stream()
                    .map(GrantedAuthority::getAuthority)
                    .anyMatch("EMP"::equals);

            /**
             * 【EMPロールに対する特別処理】
             * EMPユーザーが管理者専用リソースにアクセスしようとした場合、エラーではなくリダイレクトを実行します。
             */
            if (isEmp) {
                // EMPは /html/emp_main.html に強制的にリダイレクト
                response.sendRedirect("/html/emp_main.html");
                return;
            }
        }

        // それ以外の場合（未認証ユーザー、またはその他の権限不足ユーザー）は、デフォルトの403エラーを返す
        response.sendError(HttpServletResponse.SC_FORBIDDEN, "Access Denied"); // 403 Forbidden
    }
}