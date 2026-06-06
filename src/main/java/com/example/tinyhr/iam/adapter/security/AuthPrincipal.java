package com.example.tinyhr.iam.adapter.security;

/**
 * 인증된 요청 주체. JWT 필터가 SecurityContext 에 심고, 컨트롤러는
 * {@code @AuthenticationPrincipal AuthPrincipal} 로 받는다.
 *
 * @param userAccountId 사원/계정 식별자(JWT sub)
 * @param sessionId 세션 식별자(로그아웃 단위)
 */
public record AuthPrincipal(String userAccountId, String sessionId) {}
