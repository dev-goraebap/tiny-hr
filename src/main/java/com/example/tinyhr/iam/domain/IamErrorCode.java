package com.example.tinyhr.iam.domain;

import com.example.tinyhr.shared.kernel.ErrorCode;
import org.springframework.http.HttpStatus;

/** iam 컨텍스트 에러 카탈로그. */
public enum IamErrorCode implements ErrorCode {

    // user-account
    USER_ACCOUNT_NOT_FOUND(HttpStatus.NOT_FOUND, "인증 계정을 찾을 수 없습니다"),
    USER_ACCOUNT_EMAIL_DUPLICATED(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다");

    private final HttpStatus status;
    private final String message;

    IamErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    @Override
    public String code() {
        return name();
    }

    @Override
    public HttpStatus status() {
        return status;
    }

    @Override
    public String message() {
        return message;
    }
}
