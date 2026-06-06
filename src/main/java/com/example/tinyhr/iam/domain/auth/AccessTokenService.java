package com.example.tinyhr.iam.domain.auth;

/**
 * 액세스 토큰(JWT) 서명·검증 포트. 구현은 어댑터에서 수행한다.
 *
 * <p>리프레시 토큰은 별도의 해시 문자열 체인이며 이 포트는 access token 만 다룬다.
 */
public interface AccessTokenService {

    /** {@code sub}(userAccountId) + {@code sessionId} 로 access token 서명. */
    String sign(String userAccountId, String sessionId);

    /** access token 검증. 유효하지 않으면 예외를 던진다. */
    AccessTokenClaims verify(String token);

    /** 검증된 access token 클레임. */
    record AccessTokenClaims(String userAccountId, String sessionId) {}
}
