package com.example.attendance.attendance_app.filter;

import com.example.attendance.attendance_app.service.JwtService;
import com.example.attendance.attendance_app.service.UserDetailsServiceImpl;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * HTTPリクエストからJWTトークンを抽出し、認証を行うためのフィルターです。
 *
 * 【機能】
 * 1. Authorizationヘッダーから「Bearer [JWT]」形式のトークンを取得します。
 * 2. トークンを検証し、有効な場合はユーザー情報（UserDetails）を読み込みます。
 * 3. ユーザー情報をSecurityContextに設定し、後続の認可処理（ロールチェックなど）を可能にします。
 *
 * 【注意事項】
 * 全てのリクエストに対して一度だけ実行されます（OncePerRequestFilter）。
 */
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsServiceImpl userDetailsService;

    /**
     * フィルター処理の本体です。
     *
     * 【機能】
     * リクエストヘッダーに有効なJWTが存在する場合、SecurityContextに認証オブジェクトを設定します。
     *
     * @param request     フィルタリング対象のHTTPリクエスト
     * @param response    HTTPレスポンス
     * @param filterChain 次のフィルターまたはエンドポイントへ処理を渡すチェーン
     * @throws ServletException サーブレット処理中のエラー
     * @throws IOException      I/Oエラー
     */
    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String employeeId;

        // 1. Authorizationヘッダーが存在しない、または "Bearer " で始まらない場合はスキップ
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 2. トークンの抽出
        jwt = authHeader.substring(7);
        employeeId = jwtService.extractUsername(jwt);

        // 3. 認証済みユーザーがおらず、従業員IDがトークンから抽出できた場合
        if (employeeId != null && SecurityContextHolder.getContext().getAuthentication() == null) {

            // ユーザー詳細情報 (UserDetails) のロード
            UserDetails userDetails = this.userDetailsService.loadUserByUsername(employeeId);

            // 4. トークンの有効性チェック
            if (jwtService.isTokenValid(jwt, userDetails)) {

                // 5. 認証オブジェクトの生成とSecurityContextへの設定
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null, // 認証済みのためパスワード（クレデンシャル）は不要
                        userDetails.getAuthorities());

                authToken.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authToken);

                System.out.println("★DEBUG: JWT認証成功! ユーザー: " + employeeId + ", ロール: " + userDetails.getAuthorities());
            } else {
                System.out.println("★DEBUG: JWTトークンが無効です。ユーザー: " + employeeId);
            }
        }

        // 6. 次のフィルターへ処理を渡す
        filterChain.doFilter(request, response);
    }
}