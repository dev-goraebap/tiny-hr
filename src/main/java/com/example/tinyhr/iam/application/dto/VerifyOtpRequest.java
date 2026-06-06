package com.example.tinyhr.iam.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** OTP 검증 요청(로그인 2단계). 코드는 6자리 숫자. */
public record VerifyOtpRequest(
        @NotBlank @Email String email,
        @NotBlank @Pattern(regexp = "\\d{6}", message = "6자리 숫자여야 합니다") String code) {}
