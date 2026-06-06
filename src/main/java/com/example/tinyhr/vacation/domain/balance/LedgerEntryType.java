package com.example.tinyhr.vacation.domain.balance;

/** 연차 원장 항목 종류. */
public enum LedgerEntryType {
    /** 관리자 부여. */
    GRANT,
    /** 휴가 승인에 따른 차감. */
    DEDUCT,
    /** 승인 후 취소에 따른 환원. */
    RESTORE
}
