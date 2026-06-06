package com.example.tinyhr.file.domain;

/**
 * 첨부 소유 대상 종류(공개 언어). 각 owner BC 가 자기 타입의 접근 인가({@code AttachmentAccessSpi})를
 * 구현·등록한다.
 */
public enum AttachmentOwnerType {
    LEAVE_REQUEST,
    LEAVE_ALLOWANCE_GRANT,
    EMPLOYEE_PROFILE,
    ANNOUNCEMENT
}
