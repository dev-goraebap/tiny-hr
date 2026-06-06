package com.example.tinyhr.vacation.domain;

import com.example.tinyhr.shared.kernel.ErrorCode;
import org.springframework.http.HttpStatus;

/** vacation 컨텍스트 에러 카탈로그. */
public enum VacationErrorCode implements ErrorCode {

    LEAVE_BALANCE_NOT_INITIALIZED(HttpStatus.BAD_REQUEST, "연차 잔액이 부여되지 않았습니다"),
    LEAVE_BALANCE_INSUFFICIENT(HttpStatus.BAD_REQUEST, "연차 잔액이 부족합니다"),
    LEAVE_GRANT_INVALID(HttpStatus.BAD_REQUEST, "부여 수량은 0보다 커야 합니다"),
    LEAVE_REQUEST_NOT_FOUND(HttpStatus.NOT_FOUND, "휴가 신청을 찾을 수 없습니다"),
    LEAVE_APPROVAL_LINE_REQUIRED(HttpStatus.BAD_REQUEST, "결재선을 1명 이상 지정해야 합니다"),
    LEAVE_PERIOD_INVALID(HttpStatus.BAD_REQUEST, "휴가 기간이 올바르지 않습니다"),
    LEAVE_TYPE_PERIOD_MISMATCH(HttpStatus.BAD_REQUEST, "반차·반반차는 하루만 신청할 수 있습니다"),
    LEAVE_CANCEL_LOCKED(HttpStatus.BAD_REQUEST, "시작일이 도래한 휴가는 취소할 수 없습니다");

    private final HttpStatus status;
    private final String message;

    VacationErrorCode(HttpStatus status, String message) {
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
