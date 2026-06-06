package com.example.tinyhr.approval.domain;

/**
 * 결재선 카테고리 — {@code approval_template.category}. 한 부서에서 카테고리당 결재선 1개만 둔다.
 */
public enum ApprovalLineCategory {
    /** 부서 공통. 카테고리별 미설정 시 fallback. */
    DEFAULT,
    /** 연차 신청. */
    ANNUAL,
    /** 특별휴가. */
    SPECIAL,
    /** 휴직(미래 대비). */
    ABSENCE,
    /** 연장근무(미래 대비). */
    OVERTIME
}
