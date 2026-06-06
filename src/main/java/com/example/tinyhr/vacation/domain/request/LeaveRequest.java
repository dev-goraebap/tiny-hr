package com.example.tinyhr.vacation.domain.request;

import com.example.tinyhr.shared.kernel.BusinessException;
import com.example.tinyhr.vacation.domain.LeaveType;
import com.example.tinyhr.vacation.domain.VacationErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 휴가 신청 상세(연차). 결재 워크플로 상태는 approval 의 {@code ApprovalRequest} 가 소유하며, 같은
 * {@code requestId} 로 연결된다(이 엔티티는 식별자를 발급하지 않고 결재 신청 식별자를 그대로 쓴다).
 *
 * <p>소비량은 신청 시점에 쿼터(0.25일) 단위 정수로 고정한다.
 */
@Entity
@Table(name = "leave_request")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LeaveRequest {

    @Id
    @Column(name = "request_id", length = 36)
    private String id;

    @Column(name = "requester_id", nullable = false, length = 36)
    private String requesterId;

    @Enumerated(EnumType.STRING)
    @Column(name = "leave_type", nullable = false, length = 20)
    private LeaveType leaveType;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    /** 소비량(쿼터 단위). */
    @Column(name = "amount_units", nullable = false)
    private int amountUnits;

    @Column(name = "reason", length = 500)
    private String reason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    /** 신규 휴가 신청 상세 생성. 식별자는 결재 신청 식별자를 그대로 받는다. */
    public static LeaveRequest create(
            String requestId,
            String requesterId,
            LeaveType leaveType,
            LocalDate startDate,
            LocalDate endDate,
            String reason,
            Instant now) {
        LeaveRequest r = new LeaveRequest();
        r.id = requestId;
        r.requesterId = requesterId;
        r.leaveType = leaveType;
        r.startDate = startDate;
        r.endDate = endDate;
        r.amountUnits = computeUnits(leaveType, startDate, endDate);
        r.reason = (reason == null || reason.isBlank()) ? null : reason.trim();
        r.createdAt = now;
        return r;
    }

    /** 시작일이 도래(오늘 이상)하면 취소할 수 없다. */
    public void assertCancellable(LocalDate today) {
        if (!today.isBefore(startDate)) {
            throw new BusinessException(VacationErrorCode.LEAVE_CANCEL_LOCKED);
        }
    }

    public double amountDays() {
        return amountUnits / 4.0;
    }

    private static int computeUnits(LeaveType leaveType, LocalDate start, LocalDate end) {
        if (end.isBefore(start)) {
            throw new BusinessException(VacationErrorCode.LEAVE_PERIOD_INVALID);
        }
        if (leaveType.isFullDay()) {
            long days = ChronoUnit.DAYS.between(start, end) + 1;
            return (int) days * LeaveType.FULL_DAY.unitsPerDay();
        }
        // 반차·반반차는 하루만
        if (!start.equals(end)) {
            throw new BusinessException(VacationErrorCode.LEAVE_TYPE_PERIOD_MISMATCH);
        }
        return leaveType.unitsPerDay();
    }
}
