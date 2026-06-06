package com.example.tinyhr.iam.domain.refreshtoken;

import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** 리프레시 토큰 리포지토리. Spring Data JPA 가 구현을 생성한다. */
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, String> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /** 세션 하나의 활성 토큰 전부 폐기(로그아웃). */
    @Modifying
    @Query("update RefreshToken t set t.revokedAt = :now "
            + "where t.sessionId = :sessionId and t.revokedAt is null")
    void revokeSession(@Param("sessionId") String sessionId, @Param("now") Instant now);

    /** 사원의 모든 세션 토큰 폐기(전체 로그아웃·강제 로그아웃·퇴사). */
    @Modifying
    @Query("update RefreshToken t set t.revokedAt = :now "
            + "where t.userAccountId = :userAccountId and t.revokedAt is null")
    void revokeAllForUser(@Param("userAccountId") String userAccountId, @Param("now") Instant now);
}
