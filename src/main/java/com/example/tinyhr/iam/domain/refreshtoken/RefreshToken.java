package com.example.tinyhr.iam.domain.refreshtoken;

import com.example.tinyhr.iam.domain.IamErrorCode;
import com.example.tinyhr.shared.kernel.BusinessException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 리프레시 토큰 애그리거트 — 회전(rotation) 체인의 단일 노드.
 *
 * <p>각 토큰은 한 번만 회전 가능하며, 회전 시 {@code rotatedToId} 에 후속 토큰 ID 를 박제한다.
 * 같은 세션({@code sessionId})의 토큰들은 로그아웃(세션 폐기) 단위를 공유한다. raw 토큰은 저장하지
 * 않고 {@code tokenHash}(sha256)만 보관한다.
 */
@Entity
@Table(name = "refresh_token")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken {

    @Id
    @Column(name = "refresh_token_id", length = 36)
    private String id;

    /** 세션 그룹 — 같은 체인의 모든 토큰이 공유. 세션 폐기 단위. */
    @Column(name = "session_id", nullable = false, length = 36)
    private String sessionId;

    @Column(name = "user_account_id", nullable = false, length = 36)
    private String userAccountId;

    @Column(name = "token_hash", nullable = false, length = 64)
    private String tokenHash;

    @Column(name = "issued_at", nullable = false)
    private Instant issuedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    /** 회전된 후속 토큰 ID. null = 미회전. */
    @Column(name = "rotated_to_id", length = 36)
    private String rotatedToId;

    /** 폐기 시각. null = 활성. */
    @Column(name = "revoked_at")
    private Instant revokedAt;

    /** 신규 토큰 발급. 식별자(refreshTokenId)·sessionId 는 호출자가 제공한다. */
    public static RefreshToken issue(
            String refreshTokenId,
            String sessionId,
            String userAccountId,
            String tokenHash,
            Instant issuedAt,
            long ttlSeconds) {
        RefreshToken token = new RefreshToken();
        token.id = refreshTokenId;
        token.sessionId = sessionId;
        token.userAccountId = userAccountId;
        token.tokenHash = tokenHash;
        token.issuedAt = issuedAt;
        token.expiresAt = issuedAt.plusSeconds(ttlSeconds);
        token.rotatedToId = null;
        token.revokedAt = null;
        return token;
    }

    public boolean isActive(Instant now) {
        return rotatedToId == null && revokedAt == null && now.isBefore(expiresAt);
    }

    /** 회전 — 후속 토큰 ID 박제. 폐기·만료면 INVALID, 이미 회전됐으면 REUSED. */
    public void rotate(String newTokenId, Instant now) {
        if (revokedAt != null) {
            throw new BusinessException(IamErrorCode.REFRESH_TOKEN_INVALID);
        }
        if (rotatedToId != null) {
            throw new BusinessException(IamErrorCode.REFRESH_TOKEN_REUSED);
        }
        if (!now.isBefore(expiresAt)) {
            throw new BusinessException(IamErrorCode.REFRESH_TOKEN_INVALID);
        }
        rotatedToId = newTokenId;
    }

    /** 멱등 폐기. 이미 폐기됐으면 no-op. */
    public void revoke(Instant now) {
        if (revokedAt != null) {
            return;
        }
        revokedAt = now;
    }
}
