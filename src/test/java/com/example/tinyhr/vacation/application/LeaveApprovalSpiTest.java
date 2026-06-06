package com.example.tinyhr.vacation.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.example.tinyhr.approval.application.spi.ApprovalDecisionContext;
import com.example.tinyhr.approval.domain.ApprovalRequestKind;
import com.example.tinyhr.approval.domain.ApprovalRequestStatus;
import com.example.tinyhr.notification.application.NotificationOpenHostService;
import com.example.tinyhr.notification.domain.NotificationKind;
import com.example.tinyhr.vacation.domain.LeaveType;
import com.example.tinyhr.vacation.domain.balance.AnnualLeaveBalance;
import com.example.tinyhr.vacation.domain.balance.AnnualLeaveBalanceRepository;
import com.example.tinyhr.vacation.domain.request.LeaveRequest;
import com.example.tinyhr.vacation.domain.request.LeaveRequestRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LeaveApprovalSpiTest {

    @Mock LeaveRequestRepository leaveRequestRepository;
    @Mock AnnualLeaveBalanceRepository annualLeaveBalanceRepository;
    @Mock NotificationOpenHostService notificationOhs;

    @InjectMocks LeaveApprovalSpi spi;

    private static final LocalDate D = LocalDate.of(2026, 2, 2);

    private static ApprovalDecisionContext ctx(ApprovalRequestStatus status, String reason) {
        return new ApprovalDecisionContext(
                "req-1", "emp", ApprovalRequestKind.LEAVE, status, List.of("a"), reason, 1);
    }

    private static LeaveRequest oneDayLeave() {
        return LeaveRequest.create("req-1", "emp", LeaveType.FULL_DAY, D, D, null, Instant.now());
    }

    private static AnnualLeaveBalance grantedBalance() {
        AnnualLeaveBalance b = AnnualLeaveBalance.init("emp");
        b.grant(40, "부여", Instant.now());
        return b;
    }

    @Test
    @DisplayName("최종 승인 시 잔액을 차감하고 승인 알림을 보낸다")
    void onApproved() {
        AnnualLeaveBalance balance = grantedBalance();
        given(leaveRequestRepository.findById("req-1")).willReturn(Optional.of(oneDayLeave()));
        given(annualLeaveBalanceRepository.findById("emp")).willReturn(Optional.of(balance));

        spi.onApproved(ctx(ApprovalRequestStatus.APPROVED, null));

        assertThat(balance.balanceDays()).isEqualTo(9.0);
        then(annualLeaveBalanceRepository).should().save(balance);
        then(notificationOhs).should().notify(
                anyList(), eq(NotificationKind.LEAVE_APPROVED), any(), any(), any());
    }

    @Test
    @DisplayName("승인 후 취소면 잔액을 환원하고 취소 알림을 보낸다")
    void onCancelled_wasApproved() {
        AnnualLeaveBalance balance = grantedBalance();
        balance.deduct(4, "차감", Instant.now()); // 9일
        given(leaveRequestRepository.findById("req-1")).willReturn(Optional.of(oneDayLeave()));
        given(annualLeaveBalanceRepository.findById("emp")).willReturn(Optional.of(balance));

        spi.onCancelled(ctx(ApprovalRequestStatus.CANCELLED, null), true);

        assertThat(balance.balanceDays()).isEqualTo(10.0);
        then(notificationOhs).should().notify(
                anyList(), eq(NotificationKind.LEAVE_CANCELLED), any(), any(), any());
    }

    @Test
    @DisplayName("승인 전 취소면 잔액 변동 없이 알림만 보낸다")
    void onCancelled_notApproved() {
        spi.onCancelled(ctx(ApprovalRequestStatus.CANCELLED, null), false);

        then(annualLeaveBalanceRepository).should(never()).save(any());
        then(notificationOhs).should().notify(
                anyList(), eq(NotificationKind.LEAVE_CANCELLED), any(), any(), any());
    }

    @Test
    @DisplayName("반려 시 반려 알림을 보낸다")
    void onRejected() {
        spi.onRejected(ctx(ApprovalRequestStatus.REJECTED, "사유"));

        then(notificationOhs).should().notify(
                anyList(), eq(NotificationKind.LEAVE_REJECTED), any(), any(), any());
        then(annualLeaveBalanceRepository).should(never()).save(any());
    }
}
