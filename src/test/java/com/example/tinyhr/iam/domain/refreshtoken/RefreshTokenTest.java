package com.example.tinyhr.iam.domain.refreshtoken;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.tinyhr.iam.domain.IamErrorCode;
import com.example.tinyhr.shared.kernel.BusinessException;
import java.time.Instant;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RefreshTokenTest {

    private static final Instant T0 = Instant.parse("2026-01-01T00:00:00Z");

    private static void assertBusiness(ThrowingCallable callable, IamErrorCode expected) {
        assertThatThrownBy(callable)
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(expected);
    }

    private static RefreshToken issued() {
        return RefreshToken.issue("tok-1", "sess-1", "user-1", "hash", T0, 3600);
    }

    @Test
    @DisplayName("발급 시 활성이다")
    void issue() {
        RefreshToken token = issued();

        assertThat(token.getSessionId()).isEqualTo("sess-1");
        assertThat(token.getExpiresAt()).isEqualTo(T0.plusSeconds(3600));
        assertThat(token.isActive(T0)).isTrue();
    }

    @Test
    @DisplayName("회전하면 후속 토큰 ID 가 박제된다")
    void rotate() {
        RefreshToken token = issued();

        token.rotate("tok-2", T0.plusSeconds(10));

        assertThat(token.getRotatedToId()).isEqualTo("tok-2");
        assertThat(token.isActive(T0.plusSeconds(10))).isFalse();
    }

    @Test
    @DisplayName("이미 회전된 토큰 재회전은 REUSED")
    void rotateAgainReused() {
        RefreshToken token = issued();
        token.rotate("tok-2", T0.plusSeconds(10));

        assertBusiness(() -> token.rotate("tok-3", T0.plusSeconds(20)),
                IamErrorCode.REFRESH_TOKEN_REUSED);
    }

    @Test
    @DisplayName("폐기된 토큰 회전은 INVALID")
    void rotateRevokedInvalid() {
        RefreshToken token = issued();
        token.revoke(T0.plusSeconds(5));

        assertBusiness(() -> token.rotate("tok-2", T0.plusSeconds(10)),
                IamErrorCode.REFRESH_TOKEN_INVALID);
    }

    @Test
    @DisplayName("만료된 토큰 회전은 INVALID")
    void rotateExpiredInvalid() {
        RefreshToken token = issued();

        assertBusiness(() -> token.rotate("tok-2", T0.plusSeconds(3600)),
                IamErrorCode.REFRESH_TOKEN_INVALID);
    }

    @Test
    @DisplayName("폐기는 멱등이다")
    void revokeIdempotent() {
        RefreshToken token = issued();

        token.revoke(T0.plusSeconds(5));
        Instant first = token.getRevokedAt();
        token.revoke(T0.plusSeconds(9));

        assertThat(token.getRevokedAt()).isEqualTo(first);
    }
}
