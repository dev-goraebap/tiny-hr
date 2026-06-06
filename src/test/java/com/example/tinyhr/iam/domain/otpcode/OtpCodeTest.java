package com.example.tinyhr.iam.domain.otpcode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.tinyhr.iam.domain.IamErrorCode;
import com.example.tinyhr.iam.domain.TokenHasher;
import com.example.tinyhr.shared.kernel.BusinessException;
import java.time.Instant;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OtpCodeTest {

    private static final Instant T0 = Instant.parse("2026-01-01T00:00:00Z");
    private static final String HASH = TokenHasher.hashToken("123456");

    private static void assertBusiness(ThrowingCallable callable, IamErrorCode expected) {
        assertThatThrownBy(callable)
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(expected);
    }

    private static OtpCode issued() {
        return OtpCode.issue("user-1", HASH, T0, 300);
    }

    @Test
    @DisplayName("발급 시 미사용·미폐기·만료시각이 설정된다")
    void issue() {
        OtpCode otp = issued();

        assertThat(otp.getId()).isNotBlank();
        assertThat(otp.getExpiresAt()).isEqualTo(T0.plusSeconds(300));
        assertThat(otp.isActive(T0)).isTrue();
        assertThat(otp.getAttempts()).isZero();
    }

    @Test
    @DisplayName("코드가 일치하면 사용 처리된다")
    void verifySuccess() {
        OtpCode otp = issued();

        otp.verify(HASH, 5, T0.plusSeconds(10));

        assertThat(otp.getConsumedAt()).isEqualTo(T0.plusSeconds(10));
        assertThat(otp.isActive(T0.plusSeconds(10))).isFalse();
    }

    @Test
    @DisplayName("코드가 틀리면 시도 횟수가 늘고 INVALID")
    void verifyWrong() {
        OtpCode otp = issued();

        assertBusiness(() -> otp.verify(TokenHasher.hashToken("000000"), 5, T0.plusSeconds(1)),
                IamErrorCode.OTP_INVALID);
        assertThat(otp.getAttempts()).isEqualTo(1);
    }

    @Test
    @DisplayName("두 번째 실패에서 최대치(2)에 도달하면 폐기된다")
    void verifyRevokeOnMax() {
        OtpCode otp = issued();
        assertBusiness(() -> otp.verify(TokenHasher.hashToken("000000"), 2, T0.plusSeconds(1)),
                IamErrorCode.OTP_INVALID);
        assertBusiness(() -> otp.verify(TokenHasher.hashToken("000000"), 2, T0.plusSeconds(2)),
                IamErrorCode.OTP_ATTEMPTS_EXCEEDED);
        assertThat(otp.getRevokedAt()).isNotNull();
        assertThat(otp.isActive(T0.plusSeconds(2))).isFalse();
    }

    @Test
    @DisplayName("만료된 코드는 EXPIRED")
    void verifyExpired() {
        OtpCode otp = issued();

        assertBusiness(() -> otp.verify(HASH, 5, T0.plusSeconds(300)), IamErrorCode.OTP_EXPIRED);
    }

    @Test
    @DisplayName("이미 사용된 코드는 ALREADY_USED")
    void verifyAlreadyUsed() {
        OtpCode otp = issued();
        otp.verify(HASH, 5, T0.plusSeconds(10));

        assertBusiness(() -> otp.verify(HASH, 5, T0.plusSeconds(11)), IamErrorCode.OTP_ALREADY_USED);
    }

    @Test
    @DisplayName("폐기는 멱등이다")
    void revokeIdempotent() {
        OtpCode otp = issued();

        otp.revoke(T0.plusSeconds(5));
        Instant first = otp.getRevokedAt();
        otp.revoke(T0.plusSeconds(9));

        assertThat(otp.getRevokedAt()).isEqualTo(first);
    }
}
