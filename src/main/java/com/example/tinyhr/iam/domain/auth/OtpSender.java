package com.example.tinyhr.iam.domain.auth;

/**
 * OTP 코드 전달 포트. 운영에서는 이메일(notification BC)로, MVP 에서는 콘솔 stub 으로 보낸다.
 */
public interface OtpSender {

    /** 사원 이메일로 6자리 OTP 코드를 보낸다. */
    void sendOtp(String email, String code, long ttlSeconds);
}
