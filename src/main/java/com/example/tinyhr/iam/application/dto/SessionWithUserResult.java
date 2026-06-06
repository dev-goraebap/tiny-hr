package com.example.tinyhr.iam.application.dto;

import java.time.Instant;

/** 로그인/개발세션 결과 — 세션 페어 + 누구의 세션인지. */
public record SessionWithUserResult(
        String userAccountId,
        String accessToken,
        String refreshToken,
        Instant refreshTokenExpiresAt) {}
