package com.example.tinyhr.approval.domain;

/**
 * 결재 요청 종류(디스크리미네이터). SPI 디스패치 키로 쓴다. 현재 휴가·연장근무.
 */
public enum ApprovalRequestKind {
    LEAVE,
    OVERTIME
}
