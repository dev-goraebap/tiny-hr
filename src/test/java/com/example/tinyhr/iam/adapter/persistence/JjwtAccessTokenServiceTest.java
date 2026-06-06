package com.example.tinyhr.iam.adapter.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.tinyhr.iam.config.AuthProperties;
import com.example.tinyhr.iam.domain.auth.AccessTokenService.AccessTokenClaims;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class JjwtAccessTokenServiceTest {

    private final JjwtAccessTokenService service = new JjwtAccessTokenService(
            new AuthProperties(300, 5, 60,
                    "test-only-secret-at-least-32-bytes-long-1234567890", 900, 3600, true));

    @Test
    @DisplayName("서명한 토큰을 검증하면 동일한 클레임이 나온다")
    void signThenVerify() {
        String token = service.sign("user-1", "sess-1");

        AccessTokenClaims claims = service.verify(token);

        assertThat(claims.userAccountId()).isEqualTo("user-1");
        assertThat(claims.sessionId()).isEqualTo("sess-1");
    }

    @Test
    @DisplayName("변조된 토큰은 검증에 실패한다")
    void rejectTampered() {
        String token = service.sign("user-1", "sess-1");
        String tampered = token.substring(0, token.length() - 2) + "xy";

        assertThatThrownBy(() -> service.verify(tampered)).isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("다른 시크릿으로 서명된 토큰은 거부된다")
    void rejectForeignSecret() {
        JjwtAccessTokenService other = new JjwtAccessTokenService(
                new AuthProperties(300, 5, 60,
                        "another-different-secret-at-least-32-bytes-xyz", 900, 3600, true));
        String foreign = other.sign("user-1", "sess-1");

        assertThatThrownBy(() -> service.verify(foreign)).isInstanceOf(RuntimeException.class);
    }
}
