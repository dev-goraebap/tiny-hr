package com.example.tinyhr.iam.adapter.persistence;

import com.example.tinyhr.iam.config.AuthProperties;
import com.example.tinyhr.iam.domain.auth.AccessTokenService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;
import io.jsonwebtoken.security.Keys;

/**
 * jjwt(HS256) 기반 {@link AccessTokenService} 구현. access token 만 서명·검증한다
 * (refresh 는 별도 해시 문자열 체인).
 */
@Component
public class JjwtAccessTokenService implements AccessTokenService {

    private static final String CLAIM_SESSION_ID = "sessionId";

    private final SecretKey key;
    private final long ttlSeconds;

    public JjwtAccessTokenService(AuthProperties props) {
        this.key = Keys.hmacShaKeyFor(props.jwtAccessSecret().getBytes(StandardCharsets.UTF_8));
        this.ttlSeconds = props.jwtAccessTtlSeconds();
    }

    @Override
    public String sign(String userAccountId, String sessionId) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userAccountId)
                .claim(CLAIM_SESSION_ID, sessionId)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(ttlSeconds)))
                .signWith(key)
                .compact();
    }

    @Override
    public AccessTokenClaims verify(String token) {
        // 서명/만료 검증 실패 시 jjwt 가 예외를 던진다 — 필터가 잡아 미인증으로 처리한다.
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        String sub = claims.getSubject();
        String sessionId = claims.get(CLAIM_SESSION_ID, String.class);
        if (sub == null || sessionId == null) {
            throw new IllegalArgumentException("INVALID_ACCESS_TOKEN_PAYLOAD");
        }
        return new AccessTokenClaims(sub, sessionId);
    }
}
