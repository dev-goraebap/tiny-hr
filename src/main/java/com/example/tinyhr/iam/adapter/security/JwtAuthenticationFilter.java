package com.example.tinyhr.iam.adapter.security;

import com.example.tinyhr.iam.application.RbacOpenHostService;
import com.example.tinyhr.iam.domain.auth.AccessTokenService;
import com.example.tinyhr.iam.domain.auth.AccessTokenService.AccessTokenClaims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Bearer access token 을 검증해 {@link AuthPrincipal} 을 SecurityContext 에 심는다.
 *
 * <p>유효 토큰이면 사원의 실효 권한(RBAC)을 GrantedAuthority(permission 이름)로 실어, 이후
 * {@code SecurityConfig} 의 {@code hasAuthority(...)} 매칭이 권한 가드 역할을 한다. 토큰이 없거나
 * 검증 실패면 미인증으로 통과시키고, 보호 경로는 시큐리티가 401/403 으로 막는다.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final AccessTokenService accessTokenService;
    private final RbacOpenHostService rbacOpenHostService;

    public JwtAuthenticationFilter(
            AccessTokenService accessTokenService, RbacOpenHostService rbacOpenHostService) {
        this.accessTokenService = accessTokenService;
        this.rbacOpenHostService = rbacOpenHostService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String token = extractBearer(request);
        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                AccessTokenClaims claims = accessTokenService.verify(token);
                List<SimpleGrantedAuthority> authorities =
                        rbacOpenHostService.listEffective(claims.userAccountId()).stream()
                                .map(p -> new SimpleGrantedAuthority(p.name()))
                                .toList();
                AuthPrincipal principal =
                        new AuthPrincipal(claims.userAccountId(), claims.sessionId());
                var authentication =
                        UsernamePasswordAuthenticationToken.authenticated(principal, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (Exception e) {
                // 검증 실패: 미인증으로 통과 — 보호 경로는 시큐리티가 막는다.
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request, response);
    }

    private static String extractBearer(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            String token = header.substring(BEARER_PREFIX.length()).trim();
            if (!token.isEmpty()) {
                return token;
            }
        }
        return null;
    }
}
