package com.example.tinyhr.vacation.domain.request;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.tinyhr.shared.kernel.BusinessException;
import com.example.tinyhr.vacation.domain.LeaveType;
import com.example.tinyhr.vacation.domain.VacationErrorCode;
import java.time.Instant;
import java.time.LocalDate;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class LeaveRequestTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");
    private static final LocalDate D1 = LocalDate.of(2026, 2, 2);
    private static final LocalDate D3 = LocalDate.of(2026, 2, 4);

    private static void assertBusiness(ThrowingCallable callable, VacationErrorCode expected) {
        assertThatThrownBy(callable)
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(expected);
    }

    private static LeaveRequest create(LeaveType type, LocalDate start, LocalDate end) {
        return LeaveRequest.create("req-1", "emp", type, start, end, null, NOW);
    }

    @Test
    @DisplayName("종일 휴가는 일수 × 4쿼터로 계산된다")
    void fullDayMultiDay() {
        LeaveRequest r = create(LeaveType.FULL_DAY, D1, D3); // 3일

        assertThat(r.getAmountUnits()).isEqualTo(12);
        assertThat(r.amountDays()).isEqualTo(3.0);
    }

    @Test
    @DisplayName("반차는 0.5일(2쿼터)")
    void halfDay() {
        LeaveRequest r = create(LeaveType.HALF_DAY_AM, D1, D1);

        assertThat(r.getAmountUnits()).isEqualTo(2);
        assertThat(r.amountDays()).isEqualTo(0.5);
    }

    @Test
    @DisplayName("반차를 여러 날로 신청하면 거부된다")
    void halfDayMultiDay() {
        assertBusiness(() -> create(LeaveType.HALF_DAY_AM, D1, D3),
                VacationErrorCode.LEAVE_TYPE_PERIOD_MISMATCH);
    }

    @Test
    @DisplayName("종료일이 시작일보다 빠르면 거부된다")
    void invalidPeriod() {
        assertBusiness(() -> create(LeaveType.FULL_DAY, D3, D1),
                VacationErrorCode.LEAVE_PERIOD_INVALID);
    }

    @Test
    @DisplayName("시작일 전이면 취소할 수 있다")
    void cancellableBeforeStart() {
        LeaveRequest r = create(LeaveType.FULL_DAY, D1, D1);

        r.assertCancellable(D1.minusDays(1)); // 예외 없음
    }

    @Test
    @DisplayName("시작일이 도래하면 취소할 수 없다")
    void notCancellableOnStart() {
        LeaveRequest r = create(LeaveType.FULL_DAY, D1, D1);

        assertBusiness(() -> r.assertCancellable(D1), VacationErrorCode.LEAVE_CANCEL_LOCKED);
    }
}
