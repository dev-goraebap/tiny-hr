package com.example.tinyhr.organization.domain;

import com.example.tinyhr.shared.kernel.ErrorCode;
import org.springframework.http.HttpStatus;

/** organization 컨텍스트 에러 카탈로그. */
public enum OrganizationErrorCode implements ErrorCode {

    // rank
    RANK_NOT_FOUND(HttpStatus.NOT_FOUND, "직급을 찾을 수 없습니다"),
    RANK_NAME_DUPLICATED(HttpStatus.CONFLICT, "이미 같은 이름의 직급이 있습니다"),
    RANK_ORDER_DUPLICATED(HttpStatus.CONFLICT, "이미 같은 정렬 순서의 직급이 있습니다"),
    RANK_REORDER_DUPLICATE_ID(HttpStatus.BAD_REQUEST, "재정렬 입력에 같은 직급 ID 가 중복되었습니다"),
    RANK_REORDER_INCOMPLETE(HttpStatus.BAD_REQUEST, "활성 직급 전체가 재정렬 입력에 포함되어야 합니다"),
    RANK_INVALID(HttpStatus.BAD_REQUEST, "유효하지 않거나 비활성인 직급입니다"),

    // department
    DEPARTMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "부서를 찾을 수 없습니다"),
    DEPARTMENT_INVALID(HttpStatus.BAD_REQUEST, "유효하지 않거나 비활성인 부서입니다"),
    DEPARTMENT_PARENT_NOT_FOUND(HttpStatus.NOT_FOUND, "상위 부서를 찾을 수 없습니다"),
    DEPARTMENT_NAME_DUPLICATED(HttpStatus.CONFLICT, "같은 상위에 같은 이름의 부서가 있습니다"),
    DEPARTMENT_DEPTH_EXCEEDED(HttpStatus.BAD_REQUEST, "부서 트리는 2뎁스까지만 허용됩니다"),
    DEPARTMENT_PARENT_INACTIVE(HttpStatus.BAD_REQUEST, "비활성 상위 부서 아래에는 생성할 수 없습니다"),
    DEPARTMENT_ARCHIVED(HttpStatus.BAD_REQUEST, "아카이브된 부서는 수정할 수 없습니다"),
    DEPARTMENT_HAS_ACTIVE_CHILDREN(HttpStatus.CONFLICT, "활성 하위 부서가 있어 아카이브할 수 없습니다"),
    DEPARTMENT_REORDER_DUPLICATED(HttpStatus.BAD_REQUEST, "재정렬 입력에 같은 부서 ID 가 중복되었습니다"),
    DEPARTMENT_REORDER_PARENT_MISMATCH(HttpStatus.BAD_REQUEST, "재정렬 대상이 지정한 상위에 속하지 않습니다"),

    // employee
    EMPLOYEE_NOT_FOUND(HttpStatus.NOT_FOUND, "사원을 찾을 수 없습니다"),
    EMPLOYEE_EMAIL_DUPLICATED(HttpStatus.CONFLICT, "이미 같은 업무 이메일의 사원이 있습니다"),
    EMPLOYEE_ALREADY_ACTIVE(HttpStatus.CONFLICT, "이미 활성화된 사원입니다"),
    EMPLOYEE_ALREADY_TERMINATED(HttpStatus.CONFLICT, "이미 퇴사 처리된 사원입니다"),
    EMPLOYEE_INVALID_STATUS_TRANSITION(HttpStatus.BAD_REQUEST, "허용되지 않는 사원 상태 전이입니다"),
    EMPLOYEE_TERMINATED_READ_ONLY(HttpStatus.BAD_REQUEST, "퇴사 처리된 사원은 수정할 수 없습니다"),
    EMPLOYEE_NOT_ACTIVE(HttpStatus.BAD_REQUEST, "재직 중인 사원이 아닙니다"),
    EMPLOYEE_NAME_INVALID(HttpStatus.BAD_REQUEST, "사원 이름이 유효하지 않습니다"),
    EMPLOYEE_WORK_EMAIL_INVALID(HttpStatus.BAD_REQUEST, "업무 이메일 형식이 유효하지 않습니다");

    private final HttpStatus status;
    private final String message;

    OrganizationErrorCode(HttpStatus status, String message) {
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
