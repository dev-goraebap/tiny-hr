package com.example.tinyhr.vacation.application;

import com.example.tinyhr.vacation.domain.VacationErrorCode;
import com.example.tinyhr.vacation.domain.balance.AnnualLeaveBalance;
import com.example.tinyhr.vacation.domain.balance.AnnualLeaveBalanceRepository;
import com.example.tinyhr.shared.kernel.BusinessException;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 사원 연차 잔액을 부여한다(관리자). 잔액 차감·환원은 휴가 결재 확정 후속({@code LeaveApprovalSpi})이
 * 담당한다.
 *
 * @actor 관리자
 */
@Service
@Transactional
public class AnnualLeaveBalanceService {

    private final AnnualLeaveBalanceRepository annualLeaveBalanceRepository;

    public AnnualLeaveBalanceService(AnnualLeaveBalanceRepository annualLeaveBalanceRepository) {
        this.annualLeaveBalanceRepository = annualLeaveBalanceRepository;
    }

    /** 사원에게 연차를 부여한다. 잔액 홀더가 없으면 생성한다. */
    public void grant(String employeeId, double days, String reason) {
        int units = (int) Math.round(days * 4);
        if (units <= 0) {
            throw new BusinessException(VacationErrorCode.LEAVE_GRANT_INVALID);
        }
        AnnualLeaveBalance balance = annualLeaveBalanceRepository.findById(employeeId)
                .orElseGet(() -> AnnualLeaveBalance.init(employeeId));
        balance.grant(units, reason, Instant.now());
        annualLeaveBalanceRepository.save(balance);
    }
}
