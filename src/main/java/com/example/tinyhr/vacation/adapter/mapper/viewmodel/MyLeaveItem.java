package com.example.tinyhr.vacation.adapter.mapper.viewmodel;

import java.time.Instant;
import java.time.LocalDate;

/**
 * 내 휴가 신청 목록 조회 뷰. 결재 상태(status)는 approval 의 approval_request 를 조인해 가져온다
 * (읽기 측 프로젝션).
 */
public record MyLeaveItem(
        String requestId,
        String leaveType,
        LocalDate startDate,
        LocalDate endDate,
        double amountDays,
        String status,
        Instant createdAt) {}
