package com.example.tinyhr.iam.domain.otpcode;

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
 * OTP 코드 애그리거트 — 이메일로 발송되는 6자리 인증번호.
 *
 * <p>raw 6자리 코드는 한 번만 발행되어 이메일 본문에 들어가고, DB 에는 sha256 해시만 저장한다.
 * 한 사원에게 active(미사용·미폐기·미만료) OTP 는 1건만 유지한다(새 발급 시 기존 것을 폐기).
 * 검증 {@code maxAttempts} 초과 시 자동 폐기되어 재발급이 필요하다.
 */
@Entity
@Table(name = "otp_code")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OtpCode {

    @Id
    @Column(name = "otp_code_id", length = 36)
    private String id;

    @Column(name = "user_account_id", nullable = false, length = 36)
    private String userAccountId;

    /** sha256(raw 6자리 코드) hex. raw 는 저장하지 않는다. */
    @Column(name = "code_hash", nullable = false, length = 64)
    private String codeHash;

    @Column(name = "issued_at", nullable = false)
    private Instant issuedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    /** 사용 시각. verify 성공이 채운다. null = 미사용. */
    @Column(name = "consumed_at")
    private Instant consumedAt;

    /** 폐기 시각. 재발급/시도초과가 채운다. null = 미폐기. */
    @Column(name = "revoked_at")
    private Instant revokedAt;

    /** 검증 시도 횟수. */
    @Column(nullable = false)
    private int attempts;

    /** 신규 OTP 발급. 식별자는 도메인이 발급한다. */
    public static OtpCode issue(
            String userAccountId, String codeHash, Instant issuedAt, long ttlSeconds) {
        OtpCode otp = new OtpCode();
        otp.id = UUID.randomUUID().toString();
        otp.userAccountId = userAccountId;
        otp.codeHash = codeHash;
        otp.issuedAt = issuedAt;
        otp.expiresAt = issuedAt.plusSeconds(ttlSeconds);
        otp.consumedAt = null;
        otp.revokedAt = null;
        otp.attempts = 0;
        return otp;
    }

    /**
     * 시도 회차 1 증가 + 검증 분기.
     *
     * <ul>
     *   <li>이미 사용/폐기/만료면 도메인 에러</li>
     *   <li>해시 불일치면 attempts 증가 후 에러(최대치 초과면 폐기까지)</li>
     *   <li>일치면 consumedAt 설정</li>
     * </ul>
     */
    public void verify(String expectedHash, int maxAttempts, Instant now) {
        if (consumedAt != null) {
            throw new BusinessException(IamErrorCode.OTP_ALREADY_USED);
        }
        if (revokedAt != null) {
            throw new BusinessException(IamErrorCode.OTP_REVOKED);
        }
        if (!now.isBefore(expiresAt)) {
            throw new BusinessException(IamErrorCode.OTP_EXPIRED);
        }
        if (!expectedHash.equals(codeHash)) {
            attempts += 1;
            if (attempts >= maxAttempts) {
                revokedAt = now;
                throw new BusinessException(IamErrorCode.OTP_ATTEMPTS_EXCEEDED);
            }
            throw new BusinessException(IamErrorCode.OTP_INVALID);
        }
        consumedAt = now;
    }

    /** 재발급 시 기존 OTP 무효화. 이미 사용/폐기면 멱등. */
    public void revoke(Instant now) {
        if (consumedAt != null || revokedAt != null) {
            return;
        }
        revokedAt = now;
    }

    /** active = 미사용 + 미폐기 + 미만료. 쿨다운·검증 대상 판정에 사용. */
    public boolean isActive(Instant now) {
        return consumedAt == null && revokedAt == null && now.isBefore(expiresAt);
    }
}
