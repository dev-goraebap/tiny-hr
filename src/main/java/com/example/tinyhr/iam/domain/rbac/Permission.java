package com.example.tinyhr.iam.domain.rbac;

/**
 * 시스템 전역 권한 카탈로그(공개 언어, Published Language).
 *
 * <p>RBAC 설계상 Role 이 이 enum 의 부분집합을 보유하며, 사원은 여러 Role 을 갖고 합집합(Union)으로
 * 권한이 병합된다. 권한 판정은 {@code RbacOpenHostService.has}, 화면용 전체 권한은
 * {@code RbacOpenHostService.listEffective} 로 노출한다.
 */
public enum Permission {

    /** 관리자페이지 일반 제어(사원·부서·결재양식·휴가유형·연차·근태조회 등). */
    ADMIN_PAGE_CONTROL,

    /** 사원 인사카드 상세 조회(민감). 관리자페이지 제어와 함께 요구. */
    ADMIN_PROFILE_SENSITIVE,

    /** 역할/권한 관리. */
    ROLE_MANAGE,

    /** 집중근로 신청·조회 — PM. */
    OVERTIME_CONTROL
}
