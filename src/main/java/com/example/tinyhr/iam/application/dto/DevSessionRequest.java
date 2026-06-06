package com.example.tinyhr.iam.application.dto;

import jakarta.validation.constraints.NotBlank;

/** 개발 전용 무인증 세션 발급 요청. */
public record DevSessionRequest(@NotBlank String userAccountId) {}
