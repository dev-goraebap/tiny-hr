package com.example.tinyhr.vacation.application;

import com.example.tinyhr.approval.application.ApprovalOpenHostService;
import com.example.tinyhr.approval.application.ApprovalOpenHostService.ApprovalSubmissionResult;
import com.example.tinyhr.approval.domain.ApprovalRequestKind;
import com.example.tinyhr.notification.application.NotificationOpenHostService;
import com.example.tinyhr.notification.domain.NotificationKind;
import com.example.tinyhr.shared.kernel.BusinessException;
import com.example.tinyhr.vacation.application.dto.CreateLeaveRequest;
import com.example.tinyhr.vacation.domain.VacationErrorCode;
import com.example.tinyhr.vacation.domain.balance.AnnualLeaveBalance;
import com.example.tinyhr.vacation.domain.balance.AnnualLeaveBalanceRepository;
import com.example.tinyhr.vacation.domain.request.LeaveRequest;
import com.example.tinyhr.vacation.domain.request.LeaveRequestRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 휴가(연차) 신청을 생성한다. 잔액 사전 검증 후 결재 신청({@link ApprovalOpenHostService})을 올리고,
 * 결재자가 없으면 즉시 자동 승인한다(이때 {@code LeaveApprovalSpi.onApproved} 로 잔액이 차감된다).
 *
 * @actor 일반사원
 */
@Service
@Transactional
public class LeaveRequestService {

    private final LeaveRequestRepository leaveRequestRepository;
    private final AnnualLeaveBalanceRepository annualLeaveBalanceRepository;
    private final ApprovalOpenHostService approvalOhs;
    private final NotificationOpenHostService notificationOhs;

    public LeaveRequestService(
            LeaveRequestRepository leaveRequestRepository,
            AnnualLeaveBalanceRepository annualLeaveBalanceRepository,
            ApprovalOpenHostService approvalOhs,
            NotificationOpenHostService notificationOhs) {
        this.leaveRequestRepository = leaveRequestRepository;
        this.annualLeaveBalanceRepository = annualLeaveBalanceRepository;
        this.approvalOhs = approvalOhs;
        this.notificationOhs = notificationOhs;
    }

    /** 휴가 신청을 생성하고 결재를 올린다. 생성된 신청 식별자를 돌려준다. */
    public String create(String requesterId, CreateLeaveRequest request) {
        if (request.approvalLine().isEmpty()) {
            throw new BusinessException(VacationErrorCode.LEAVE_APPROVAL_LINE_REQUIRED);
        }

        String requestId = UUID.randomUUID().toString();
        LeaveRequest detail = LeaveRequest.create(
                requestId, requesterId, request.leaveType(),
                request.startDate(), request.endDate(), request.reason(), Instant.now());

        AnnualLeaveBalance balance = annualLeaveBalanceRepository.findById(requesterId)
                .orElseThrow(() ->
                        new BusinessException(VacationErrorCode.LEAVE_BALANCE_NOT_INITIALIZED));
        if (!balance.canCover(detail.getAmountUnits())) {
            throw new BusinessException(VacationErrorCode.LEAVE_BALANCE_INSUFFICIENT);
        }

        leaveRequestRepository.save(detail);

        ApprovalSubmissionResult result = approvalOhs.submit(
                requestId, requesterId, ApprovalRequestKind.LEAVE, request.approvalLine());

        if (result.autoApproved()) {
            // 결재자가 없으면 즉시 확정 — LeaveApprovalSpi.onApproved 가 잔액을 차감한다.
            approvalOhs.finalizeAutoApproval(requestId, requesterId);
        } else {
            notificationOhs.notify(
                    result.approvers(), NotificationKind.LEAVE_SUBMITTED,
                    "새 결재 요청이 도착했어요",
                    String.format("%.2f일 휴가 신청이 결재 대기 중입니다.", detail.amountDays()),
                    "/approvals");
        }
        return requestId;
    }
}
