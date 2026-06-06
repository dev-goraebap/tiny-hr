package com.example.tinyhr.iam.domain;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * 토큰 해시 헬퍼 — OTP 코드 / Refresh Token 은 raw 가 아니라 해시만 DB 에 저장한다.
 *
 * <p>단순 SHA-256(페퍼 없음) — refresh 토큰은 32바이트 랜덤으로 엔트로피가 충분하고, OTP 는
 * 짧은 TTL·시도제한으로 보호된다.
 */
public final class TokenHasher {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();

    private TokenHasher() {}

    /** URL-safe 랜덤 토큰(기본 32바이트). refresh 토큰 raw 값 생성용. */
    public static String generateRawToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return URL_ENCODER.encodeToString(bytes);
    }

    /** 6자리 OTP 코드(000000~999999, 앞 0 패딩). 균등 분포. */
    public static String generateOtpCode() {
        return String.format("%06d", RANDOM.nextInt(1_000_000));
    }

    /** sha256(raw) hex. */
    public static String hashToken(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
