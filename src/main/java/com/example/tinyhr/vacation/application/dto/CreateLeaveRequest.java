package com.example.tinyhr.vacation.application.dto;

import com.example.tinyhr.vacation.domain.LeaveType;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

/** 휴가 신청 입력(연차). 결재선은 1명 이상. 기간 형식·금액은 도메인이 검증·계산. */
public record CreateLeaveRequest(
        @NotNull LeaveType leaveType,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate,
        String reason,
        @NotEmpty List<String> approvalLine) {}
