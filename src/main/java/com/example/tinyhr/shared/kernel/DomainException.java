package com.example.tinyhr.shared.kernel;

/**
 * 도메인 규칙 위반을 표현하는 예외의 베이스.
 *
 * 메시지 첫 토큰(":" 앞)을 응답 {@code code} 로 노출한다.
 * 예) "POSITION_INVALID: 비활성 직위" -> code "POSITION_INVALID".
 */
public abstract class DomainException extends RuntimeException {

    protected DomainException(String message) {
        super(message);
    }

    public String code() {
        String message = getMessage();
        if (message == null) {
            return getClass().getSimpleName();
        }
        int idx = message.indexOf(':');
        return idx > 0 ? message.substring(0, idx).trim() : message;
    }
}
