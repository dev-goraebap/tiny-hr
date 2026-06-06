package com.example.tinyhr.iam.application.dto;

import jakarta.validation.constraints.NotBlank;

/** 리프레시 토큰 회전 요청. */
public record RefreshTokenRequest(@NotBlank String rawRefreshToken) {}
