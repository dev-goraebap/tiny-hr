package com.example.tinyhr.iam.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 인증(iam) 설정. {@code tinyhr.auth.*} 프로퍼티로 주입된다.
 *
 * @param otpTtlSeconds OTP 유효시간(초)
 * @param otpMaxAttempts OTP 검증 최대 시도 횟수(초과 시 자동 폐기)
 * @param otpResendCooldownSeconds OTP 재발급 쿨다운(초). 0 이면 비활성
 * @param jwtAccessSecret access token HS256 서명 시크릿(운영은 환경변수로 주입)
 * @param jwtAccessTtlSeconds access token 유효시간(초)
 * @param jwtRefreshTtlSeconds refresh token 유효시간(초)
 * @param devLoginEnabled 개발용 무인증 세션 발급 허용 여부(운영 false)
 */
@ConfigurationProperties(prefix = "tinyhr.auth")
public record AuthProperties(
        long otpTtlSeconds,
        int otpMaxAttempts,
        long otpResendCooldownSeconds,
        String jwtAccessSecret,
        long jwtAccessTtlSeconds,
        long jwtRefreshTtlSeconds,
        boolean devLoginEnabled) {}
