package com.example.tinyhr.vacation.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.example.tinyhr.approval.application.ApprovalOpenHostService;
import com.example.tinyhr.approval.application.ApprovalOpenHostService.ApprovalSubmissionResult;
import com.example.tinyhr.approval.domain.ApprovalRequestKind;
import com.example.tinyhr.notification.application.NotificationOpenHostService;
import com.example.tinyhr.notification.domain.NotificationKind;
import com.example.tinyhr.shared.kernel.BusinessException;
import com.example.tinyhr.vacation.application.dto.CreateLeaveRequest;
import com.example.tinyhr.vacation.domain.LeaveType;
import com.example.tinyhr.vacation.domain.VacationErrorCode;
import com.example.tinyhr.vacation.domain.balance.AnnualLeaveBalance;
import com.example.tinyhr.vacation.domain.balance.AnnualLeaveBalanceRepository;
import com.example.tinyhr.vacation.domain.request.LeaveRequest;
import com.example.tinyhr.vacation.domain.request.LeaveRequestRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LeaveRequestServiceTest {

    @Mock LeaveRequestRepository leaveRequestRepository;
    @Mock AnnualLeaveBalanceRepository annualLeaveBalanceRepository;
    @Mock ApprovalOpenHostService approvalOhs;
    @Mock NotificationOpenHostService notificationOhs;

    @InjectMocks LeaveRequestService leaveRequestService;

    private static final LocalDate D = LocalDate.of(2026, 2, 2);

    private static CreateLeaveRequest req(List<String> line) {
        return new CreateLeaveRequest(LeaveType.FULL_DAY, D, D, null, line);
    }

    private static AnnualLeaveBalance balanceWithDays(int days) {
        AnnualLeaveBalance b = AnnualLeaveBalance.init("emp");
        b.grant(days * 4, "부여", Instant.now());
        return b;
    }

    private static void assertBusiness(ThrowingCallable callable, VacationErrorCode expected) {
        assertThatThrownBy(callable)
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(expected);
    }

    @Test
    @DisplayName("결재선이 비면 신청할 수 없다")
    void create_emptyLine() {
        assertBusiness(() -> leaveRequestService.create("emp", req(List.of())),
                VacationErrorCode.LEAVE_APPROVAL_LINE_REQUIRED);
    }

    @Test
    @DisplayName("잔액 홀더가 없으면 신청할 수 없다")
    void create_balanceNotInitialized() {
        given(annualLeaveBalanceRepository.findById("emp")).willReturn(Optional.empty());

        assertBusiness(() -> leaveRequestService.create("emp", req(List.of("a"))),
                VacationErrorCode.LEAVE_BALANCE_NOT_INITIALIZED);
    }

    @Test
    @DisplayName("잔액이 부족하면 신청할 수 없다")
    void create_insufficient() {
        AnnualLeaveBalance halfDay = AnnualLeaveBalance.init("emp");
        halfDay.grant(2, "부여", Instant.now()); // 0.5일
        given(annualLeaveBalanceRepository.findById("emp")).willReturn(Optional.of(halfDay));

        // 1일(4쿼터) 신청 → 잔액 0.5일로 부족
        assertBusiness(() -> leaveRequestService.create("emp", req(List.of("a"))),
                VacationErrorCode.LEAVE_BALANCE_INSUFFICIENT);
        then(leaveRequestRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("결재자가 있으면 결재 신청을 올리고 결재자에게 알린다")
    void create_withApprovers() {
        given(annualLeaveBalanceRepository.findById("emp"))
                .willReturn(Optional.of(balanceWithDays(10)));
        given(approvalOhs.submit(anyString(), eq("emp"), eq(ApprovalRequestKind.LEAVE), any()))
                .willReturn(new ApprovalSubmissionResult(false, List.of("a")));

        String id = leaveRequestService.create("emp", req(List.of("a")));

        assertThat(id).isNotBlank();
        then(leaveRequestRepository).should().save(any(LeaveRequest.class));
        then(notificationOhs).should().notify(
                anyList(), eq(NotificationKind.LEAVE_SUBMITTED), any(), any(), any());
        then(approvalOhs).should(never()).finalizeAutoApproval(anyString(), anyString());
    }

    @Test
    @DisplayName("결재자가 없으면 자동 승인 확정을 호출한다")
    void create_autoApproved() {
        given(annualLeaveBalanceRepository.findById("emp"))
                .willReturn(Optional.of(balanceWithDays(10)));
        given(approvalOhs.submit(anyString(), eq("emp"), eq(ApprovalRequestKind.LEAVE), any()))
                .willReturn(new ApprovalSubmissionResult(true, List.of("emp")));

        leaveRequestService.create("emp", req(List.of("emp")));

        then(approvalOhs).should().finalizeAutoApproval(anyString(), eq("emp"));
        then(notificationOhs).should(never()).notify(anyList(), any(), any(), any(), any());
    }
}
