package com.example.tinyhr.vacation.domain.balance;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 연차 잔액 변동 원장 항목(불변 값). units 는 쿼터(0.25일) 단위 부호 있는 정수. */
@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LeaveLedgerEntry {

    @Enumerated(EnumType.STRING)
    @Column(name = "entry_type", length = 16)
    private LedgerEntryType type;

    /** 부호 있는 변동량(부여·환원 +, 차감 -). */
    @Column(name = "units")
    private int units;

    @Column(name = "reason", length = 200)
    private String reason;

    @Column(name = "occurred_at")
    private Instant occurredAt;

    static LeaveLedgerEntry of(LedgerEntryType type, int units, String reason, Instant occurredAt) {
        LeaveLedgerEntry e = new LeaveLedgerEntry();
        e.type = type;
        e.units = units;
        e.reason = reason;
        e.occurredAt = occurredAt;
        return e;
    }
}
