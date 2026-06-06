package com.example.tinyhr.iam.adapter.persistence;

import com.example.tinyhr.iam.domain.auth.OtpSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * OTP 발송 MVP stub — 실제 이메일 대신 콘솔(로그)로 코드를 출력한다.
 *
 * <p>개발/로컬 전용. 운영에서는 notification BC 의 이메일 발송 어댑터로 교체한다. raw 코드를 로그에
 * 남기므로 운영 사용 금지.
 */
@Component
public class LoggingOtpSender implements OtpSender {

    private static final Logger log = LoggerFactory.getLogger(LoggingOtpSender.class);

    @Override
    public void sendOtp(String email, String code, long ttlSeconds) {
        long ttlMinutes = Math.max(1, Math.round(ttlSeconds / 60.0));
        log.info("[OTP-STUB] to={} code={} (유효 {}분) — 실제 이메일 발송 아님", email, code, ttlMinutes);
    }
}
