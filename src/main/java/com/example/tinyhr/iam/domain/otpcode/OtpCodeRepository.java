package com.example.tinyhr.iam.domain.otpcode;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** OTP 코드 리포지토리. Spring Data JPA 가 구현을 생성한다. */
public interface OtpCodeRepository extends JpaRepository<OtpCode, String> {

    /** 사원의 미사용·미폐기 OTP 중 가장 최근 것. (만료 여부는 도메인 isActive 로 판정) */
    Optional<OtpCode> findFirstByUserAccountIdAndConsumedAtIsNullAndRevokedAtIsNullOrderByIssuedAtDesc(
            String userAccountId);

    /** 사원의 모든 미사용·미폐기 OTP — 재발급 시 일괄 폐기 대상. */
    List<OtpCode> findByUserAccountIdAndConsumedAtIsNullAndRevokedAtIsNull(String userAccountId);

    /** 사원의 active OTP 전부 폐기(동시 유효 OTP 1건 유지). */
    default void revokeAllActiveForUser(String userAccountId, Instant now) {
        List<OtpCode> active =
                findByUserAccountIdAndConsumedAtIsNullAndRevokedAtIsNull(userAccountId);
        active.forEach(otp -> otp.revoke(now));
        saveAll(active);
    }
}
