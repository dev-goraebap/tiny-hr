package com.example.tinyhr.approval.domain;

import com.example.tinyhr.shared.kernel.ErrorCode;
import org.springframework.http.HttpStatus;

/** approval 컨텍스트 에러 카탈로그. */
public enum ApprovalErrorCode implements ErrorCode {

    // request
    APPROVAL_REQUEST_NOT_FOUND(HttpStatus.NOT_FOUND, "결재 요청을 찾을 수 없습니다"),
    APPROVAL_REQUEST_FORBIDDEN(HttpStatus.FORBIDDEN, "본인이 신청한 요청만 처리할 수 있습니다"),
    APPROVAL_REQUEST_INVALID_TRANSITION(HttpStatus.BAD_REQUEST, "현재 상태에서 실행할 수 없는 결재 동작입니다"),
    APPROVER_MISMATCH(HttpStatus.FORBIDDEN, "현재 순서의 결재자가 아닙니다"),
    APPROVAL_DECISION_REASON_REQUIRED(HttpStatus.BAD_REQUEST, "반려 사유는 비어 있을 수 없습니다"),
    APPROVAL_LINE_INVALID(HttpStatus.BAD_REQUEST, "결재선은 1~3명이어야 합니다"),

    // template
    APPROVAL_TEMPLATE_NOT_FOUND(HttpStatus.NOT_FOUND, "결재선을 찾을 수 없습니다"),
    APPROVAL_TEMPLATE_DEPARTMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "부서를 찾을 수 없습니다"),
    APPROVAL_TEMPLATE_EMPLOYEE_NOT_FOUND(HttpStatus.NOT_FOUND, "결재자 사원을 찾을 수 없습니다"),
    APPROVAL_TEMPLATE_DUPLICATE_CATEGORY(HttpStatus.CONFLICT, "해당 부서에 같은 카테고리의 결재선이 이미 있습니다"),
    APPROVAL_TEMPLATE_INVALID_APPROVERS(
            HttpStatus.BAD_REQUEST, "결재자는 1~3명, 순서와 사원이 중복되지 않아야 합니다");

    private final HttpStatus status;
    private final String message;

    ApprovalErrorCode(HttpStatus status, String message) {
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
