package com.example.tinyhr.iam.domain;

import com.example.tinyhr.shared.kernel.ErrorCode;
import org.springframework.http.HttpStatus;

/** iam 컨텍스트 에러 카탈로그. */
public enum IamErrorCode implements ErrorCode {

    // user-account
    USER_ACCOUNT_NOT_FOUND(HttpStatus.NOT_FOUND, "인증 계정을 찾을 수 없습니다"),
    USER_ACCOUNT_EMAIL_DUPLICATED(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다"),

    // rbac - role
    ROLE_NOT_FOUND(HttpStatus.NOT_FOUND, "역할을 찾을 수 없습니다"),
    ROLE_NAME_INVALID(HttpStatus.BAD_REQUEST, "역할 이름이 유효하지 않습니다(공백 불가, 64자 이하)"),
    ROLE_NAME_DUPLICATED(HttpStatus.CONFLICT, "같은 이름의 역할이 이미 있습니다(대소문자 무시)"),
    ROLE_INACTIVE(HttpStatus.BAD_REQUEST, "비활성 역할은 수정할 수 없습니다 — 재활성화 후 수정하세요"),
    ROLE_SYSTEM_RENAME_FORBIDDEN(HttpStatus.FORBIDDEN, "시스템 역할의 이름은 변경할 수 없습니다"),
    ROLE_SYSTEM_ARCHIVE_FORBIDDEN(HttpStatus.FORBIDDEN, "시스템 역할은 아카이브할 수 없습니다"),
    ROLE_LOCKOUT_FORBIDDEN(HttpStatus.FORBIDDEN, "이 변경은 ROLE_MANAGE 권한 보유자를 모두 없애게 됩니다"),

    // rbac - role-assignment
    ROLE_ASSIGNMENT_USER_NOT_FOUND(HttpStatus.NOT_FOUND, "역할을 부여할 사원(계정)을 찾을 수 없습니다"),
    ROLE_ASSIGNMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "역할 부여 내역을 찾을 수 없습니다"),
    ROLE_ASSIGNMENT_DUPLICATED(HttpStatus.CONFLICT, "이미 해당 역할이 부여되어 있습니다"),
    ROLE_ASSIGNMENT_ALREADY_REVOKED(HttpStatus.CONFLICT, "이미 회수된 부여입니다");

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
