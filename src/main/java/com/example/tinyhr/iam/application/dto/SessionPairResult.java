package com.example.tinyhr.iam.application.dto;

import java.time.Instant;

/** 세션 발급/회전 결과 — access token + refresh raw token(1회 노출) + 만료. */
public record SessionPairResult(
        String accessToken,
        String refreshToken,
        Instant refreshTokenExpiresAt) {}
