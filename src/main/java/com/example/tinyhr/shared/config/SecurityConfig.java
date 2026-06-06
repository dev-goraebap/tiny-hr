package com.example.tinyhr.shared.config;

import com.example.tinyhr.iam.adapter.security.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * 무상태(JWT) 시큐리티 체인.
 *
 * <p>{@link JwtAuthenticationFilter} 가 Bearer 토큰을 검증해 권한(permission)을 GrantedAuthority 로
 * 싣고, 아래 경로 규칙이 {@code hasAuthority(...)} 로 권한 가드를 수행한다. 권한 enum 이름을 그대로
 * authority 로 쓰므로 {@code hasRole} 의 {@code ROLE_} 접두사 문제는 없다.
 *
 * <p>공개: 인증 진입(OTP/refresh/dev) + H2 콘솔. 그 외는 인증 필요.
 */
@Configuration
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .headers(headers ->
                        headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin))
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // 인증 진입점(미인증 허용)
                        .requestMatchers("/auth/otp/issue", "/auth/otp/verify",
                                "/auth/refresh", "/auth/dev-session", "/h2-console/**")
                        .permitAll()
                        // 역할/권한 관리 — ROLE_MANAGE
                        .requestMatchers("/admin/roles/**", "/admin/role-assignments/**")
                        .hasAuthority("ROLE_MANAGE")
                        // 그 외 관리자페이지(직급·부서·사원 등) — ADMIN_PAGE_CONTROL
                        .requestMatchers("/admin/**")
                        .hasAuthority("ADMIN_PAGE_CONTROL")
                        // 로그아웃·내 정보 등은 인증만 필요
                        .anyRequest().authenticated())
                .exceptionHandling(ex -> ex.authenticationEntryPoint(
                        (request, response, authException) ->
                                response.sendError(HttpServletResponse.SC_UNAUTHORIZED)))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
