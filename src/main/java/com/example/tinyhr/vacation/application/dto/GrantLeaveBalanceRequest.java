package com.example.tinyhr.vacation.application.dto;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** 관리자 연차 부여 입력. days 는 0.25 단위 권장(쿼터로 환산). */
public record GrantLeaveBalanceRequest(
        @Positive double days,
        @Size(max = 200) String reason) {}
