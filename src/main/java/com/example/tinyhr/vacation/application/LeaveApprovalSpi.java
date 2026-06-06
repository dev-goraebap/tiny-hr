package com.example.tinyhr.vacation.application;

import com.example.tinyhr.approval.application.spi.ApprovalDecisionContext;
import com.example.tinyhr.approval.application.spi.ApprovalDecisionSpi;
import com.example.tinyhr.approval.domain.ApprovalRequestKind;
import com.example.tinyhr.notification.application.NotificationOpenHostService;
import com.example.tinyhr.notification.domain.NotificationKind;
import com.example.tinyhr.shared.kernel.BusinessException;
import com.example.tinyhr.vacation.domain.VacationErrorCode;
import com.example.tinyhr.vacation.domain.balance.AnnualLeaveBalance;
import com.example.tinyhr.vacation.domain.balance.AnnualLeaveBalanceRepository;
import com.example.tinyhr.vacation.domain.request.LeaveRequest;
import com.example.tinyhr.vacation.domain.request.LeaveRequestRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * 휴가(LEAVE) 결재 확정 후속 처리 — approval 의 {@link ApprovalDecisionSpi} 구현.
 *
 * <p>{@code @Component} 라 스프링이 approval 의 {@code ApprovalDecisionSpiRegistry}(List 주입)에 자동
 * 등록한다. 소스 의존은 vacation → approval 단방향이며, 승인/반려/취소 시 제어 흐름만 approval →
 * vacation 으로 흐른다(의존성 역전). 승인 시 잔액을 차감하고, 승인 후 취소 시 환원하며, 모든 종료
 * 전이에서 알림을 보낸다.
 */
@Component
public class LeaveApprovalSpi implements ApprovalDecisionSpi {

    private final LeaveRequestRepository leaveRequestRepository;
    private final AnnualLeaveBalanceRepository annualLeaveBalanceRepository;
    private final NotificationOpenHostService notificationOhs;

    public LeaveApprovalSpi(
            LeaveRequestRepository leaveRequestRepository,
            AnnualLeaveBalanceRepository annualLeaveBalanceRepository,
            NotificationOpenHostService notificationOhs) {
        this.leaveRequestRepository = leaveRequestRepository;
        this.annualLeaveBalanceRepository = annualLeaveBalanceRepository;
        this.notificationOhs = notificationOhs;
    }

    @Override
    public ApprovalRequestKind kind() {
        return ApprovalRequestKind.LEAVE;
    }

    @Override
    public void assertCancellable(ApprovalDecisionContext request, LocalDate today) {
        loadDetail(request.requestId()).assertCancellable(today);
    }

    @Override
    public void onApproved(ApprovalDecisionContext request) {
        LeaveRequest detail = loadDetail(request.requestId());
        AnnualLeaveBalance balance = loadBalance(detail.getRequesterId());
        balance.deduct(detail.getAmountUnits(), "휴가 승인 차감", Instant.now());
        annualLeaveBalanceRepository.save(balance);

        notify(detail.getRequesterId(), NotificationKind.LEAVE_APPROVED,
                "연차 신청이 승인되었어요",
                String.format("%.2f일 휴가가 승인되었습니다.", detail.amountDays()));
    }

    @Override
    public void onRejected(ApprovalDecisionContext request) {
        String reason = request.rejectionReason();
        String message = (reason == null || reason.isBlank())
                ? "결재자가 신청을 반려했습니다."
                : "사유: " + reason;
        notify(request.requesterId(), NotificationKind.LEAVE_REJECTED,
                "휴가 신청이 반려되었어요", message);
    }

    @Override
    public void onCancelled(ApprovalDecisionContext request, boolean wasApproved) {
        if (wasApproved) {
            LeaveRequest detail = loadDetail(request.requestId());
            AnnualLeaveBalance balance = loadBalance(detail.getRequesterId());
            balance.restore(detail.getAmountUnits(), "승인 후 취소 환원", Instant.now());
            annualLeaveBalanceRepository.save(balance);
        }
        notify(request.requesterId(), NotificationKind.LEAVE_CANCELLED,
                "휴가 신청이 취소되었어요", "휴가 신청이 취소되었습니다.");
    }

    @Override
    public void onWithdrawn(ApprovalDecisionContext request) {
        // 승인 전 회수라 잔액 변동 없음.
        notify(request.requesterId(), NotificationKind.LEAVE_CANCELLED,
                "휴가 신청이 취소되었어요", "휴가 신청을 회수했습니다.");
    }

    private LeaveRequest loadDetail(String requestId) {
        return leaveRequestRepository.findById(requestId)
                .orElseThrow(() ->
                        new BusinessException(VacationErrorCode.LEAVE_REQUEST_NOT_FOUND));
    }

    private AnnualLeaveBalance loadBalance(String employeeId) {
        return annualLeaveBalanceRepository.findById(employeeId)
                .orElseThrow(() ->
                        new BusinessException(VacationErrorCode.LEAVE_BALANCE_NOT_INITIALIZED));
    }

    private void notify(String recipientId, NotificationKind kind, String title, String message) {
        notificationOhs.notify(List.of(recipientId), kind, title, message, "/me/leaves");
    }
}
