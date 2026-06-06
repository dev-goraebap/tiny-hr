package com.example.tinyhr.vacation.domain.balance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.tinyhr.shared.kernel.BusinessException;
import com.example.tinyhr.vacation.domain.VacationErrorCode;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AnnualLeaveBalanceTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    @Test
    @DisplayName("부여하면 잔액과 원장이 늘어난다")
    void grant() {
        AnnualLeaveBalance b = AnnualLeaveBalance.init("emp");

        b.grant(40, "초기 부여", NOW); // 10일

        assertThat(b.balanceDays()).isEqualTo(10.0);
        assertThat(b.getLedger()).hasSize(1);
    }

    @Test
    @DisplayName("0 이하 부여는 거부된다")
    void grant_nonPositive() {
        AnnualLeaveBalance b = AnnualLeaveBalance.init("emp");

        assertThatThrownBy(() -> b.grant(0, "x", NOW))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(VacationErrorCode.LEAVE_GRANT_INVALID);
    }

    @Test
    @DisplayName("잔액 내에서 차감된다")
    void deduct() {
        AnnualLeaveBalance b = AnnualLeaveBalance.init("emp");
        b.grant(40, "부여", NOW);

        b.deduct(4, "승인 차감", NOW); // 1일

        assertThat(b.balanceDays()).isEqualTo(9.0);
    }

    @Test
    @DisplayName("잔액보다 큰 차감은 거부된다")
    void deduct_insufficient() {
        AnnualLeaveBalance b = AnnualLeaveBalance.init("emp");
        b.grant(4, "부여", NOW);

        assertThatThrownBy(() -> b.deduct(8, "x", NOW))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(VacationErrorCode.LEAVE_BALANCE_INSUFFICIENT);
    }

    @Test
    @DisplayName("환원하면 잔액이 돌아온다")
    void restore() {
        AnnualLeaveBalance b = AnnualLeaveBalance.init("emp");
        b.grant(40, "부여", NOW);
        b.deduct(8, "차감", NOW);

        b.restore(8, "취소 환원", NOW);

        assertThat(b.balanceDays()).isEqualTo(10.0);
        assertThat(b.canCover(40)).isTrue();
    }
}
