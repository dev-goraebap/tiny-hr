package com.example.tinyhr.iam.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** OTP 발급 요청(로그인 1단계). */
public record IssueOtpRequest(@NotBlank @Email String email) {}
