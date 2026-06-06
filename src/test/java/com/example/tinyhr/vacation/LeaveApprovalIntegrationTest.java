package com.example.tinyhr.vacation;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.tinyhr.approval.domain.ApprovalRequestStatus;
import com.example.tinyhr.approval.domain.request.ApprovalRequest;
import com.example.tinyhr.approval.domain.request.ApprovalRequestRepository;
import com.example.tinyhr.notification.domain.delivery.NotificationDelivery;
import com.example.tinyhr.notification.domain.delivery.NotificationDeliveryRepository;
import com.example.tinyhr.vacation.application.AnnualLeaveBalanceService;
import com.example.tinyhr.vacation.application.LeaveRequestService;
import com.example.tinyhr.vacation.application.dto.CreateLeaveRequest;
import com.example.tinyhr.vacation.domain.LeaveType;
import com.example.tinyhr.vacation.domain.balance.AnnualLeaveBalance;
import com.example.tinyhr.vacation.domain.balance.AnnualLeaveBalanceRepository;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * approval · vacation · notification 세 BC 통합 검증.
 *
 * <p>결재자가 신청자뿐이라 자동 승인되는 휴가를 신청하면, approval 의 확정이 vacation 의
 * {@code LeaveApprovalSpi}(자동 등록된 SPI)를 통해 잔액 차감과 알림 발행으로 이어지는지 확인한다.
 */
@SpringBootTest
@Transactional
class LeaveApprovalIntegrationTest {

    @Autowired AnnualLeaveBalanceService annualLeaveBalanceService;
    @Autowired LeaveRequestService leaveRequestService;
    @Autowired AnnualLeaveBalanceRepository annualLeaveBalanceRepository;
    @Autowired ApprovalRequestRepository approvalRequestRepository;
    @Autowired NotificationDeliveryRepository notificationDeliveryRepository;

    @Test
    @DisplayName("자동 승인 휴가는 잔액 차감 + 결재 APPROVED + 승인 알림으로 이어진다")
    void autoApprovedLeave_endToEnd() {
        String emp = "emp-int-1";
        annualLeaveBalanceService.grant(emp, 10, "초기 부여"); // 10일

        // 결재자가 본인뿐 → 자동 승인 → LeaveApprovalSpi.onApproved 발화
        String requestId = leaveRequestService.create(emp, new CreateLeaveRequest(
                LeaveType.FULL_DAY,
                LocalDate.of(2026, 3, 2), LocalDate.of(2026, 3, 2),
                null, List.of(emp)));

        // 1) 잔액이 1일 차감되었다
        AnnualLeaveBalance balance = annualLeaveBalanceRepository.findById(emp).orElseThrow();
        assertThat(balance.balanceDays()).isEqualTo(9.0);

        // 2) 결재가 최종 승인되었다
        ApprovalRequest approval = approvalRequestRepository.findById(requestId).orElseThrow();
        assertThat(approval.getStatus()).isEqualTo(ApprovalRequestStatus.APPROVED);

        // 3) 신청자에게 승인 알림이 전달되었다
        List<NotificationDelivery> deliveries = notificationDeliveryRepository.findAll().stream()
                .filter(d -> d.getRecipientId().equals(emp))
                .toList();
        assertThat(deliveries).isNotEmpty();
    }
}
