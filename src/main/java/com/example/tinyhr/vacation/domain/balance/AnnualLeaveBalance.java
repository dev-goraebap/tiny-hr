package com.example.tinyhr.vacation.domain.balance;

import com.example.tinyhr.shared.kernel.BusinessException;
import com.example.tinyhr.vacation.domain.VacationErrorCode;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 사원별 연차 잔액 애그리거트(사원당 1개). 잔액은 쿼터(0.25일) 단위 정수이며, 모든 변동은 원장에
 * 누적된다.
 *
 * <p>부여(grant)·차감(deduct, 휴가 승인)·환원(restore, 승인 후 취소)으로만 변한다. 차감은 잔액이
 * 부족하면 거부된다.
 */
@Entity
@Table(name = "annual_leave_balance")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AnnualLeaveBalance {

    @Id
    @Column(name = "employee_id", length = 36)
    private String employeeId;

    /** 현재 잔액(쿼터 단위). */
    @Column(name = "balance_units", nullable = false)
    private int balanceUnits;

    @ElementCollection
    @CollectionTable(name = "leave_ledger_entry",
            joinColumns = @JoinColumn(name = "employee_id"))
    @OrderColumn(name = "entry_index")
    private List<LeaveLedgerEntry> ledger = new ArrayList<>();

    /** 잔액 홀더 최초 생성(잔액 0). */
    public static AnnualLeaveBalance init(String employeeId) {
        AnnualLeaveBalance b = new AnnualLeaveBalance();
        b.employeeId = employeeId;
        b.balanceUnits = 0;
        return b;
    }

    /** 관리자 부여. units 는 0보다 커야 한다. */
    public void grant(int units, String reason, Instant now) {
        if (units <= 0) {
            throw new BusinessException(VacationErrorCode.LEAVE_GRANT_INVALID);
        }
        balanceUnits += units;
        ledger.add(LeaveLedgerEntry.of(LedgerEntryType.GRANT, units, reason, now));
    }

    /** 휴가 승인 차감. 잔액이 부족하면 거부. */
    public void deduct(int units, String reason, Instant now) {
        if (balanceUnits < units) {
            throw new BusinessException(VacationErrorCode.LEAVE_BALANCE_INSUFFICIENT);
        }
        balanceUnits -= units;
        ledger.add(LeaveLedgerEntry.of(LedgerEntryType.DEDUCT, -units, reason, now));
    }

    /** 승인 후 취소 환원. */
    public void restore(int units, String reason, Instant now) {
        balanceUnits += units;
        ledger.add(LeaveLedgerEntry.of(LedgerEntryType.RESTORE, units, reason, now));
    }

    /** 잔액이 요청 수량을 감당할 수 있는지(신청 전 사전 검증용). */
    public boolean canCover(int units) {
        return balanceUnits >= units;
    }

    public double balanceDays() {
        return balanceUnits / 4.0;
    }

    public List<LeaveLedgerEntry> getLedger() {
        return Collections.unmodifiableList(ledger);
    }
}
