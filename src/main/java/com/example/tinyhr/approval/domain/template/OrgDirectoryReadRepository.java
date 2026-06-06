package com.example.tinyhr.approval.domain.template;

/**
 * 결재선 검증에 필요한 organization 데이터(사원·부서 존재) 조회 포트(읽기 전용).
 *
 * <p>organization 의 쓰기 리포지토리를 거치지 않고 approval 언어로 직접 조회한다(얕은 CQRS).
 */
public interface OrgDirectoryReadRepository {

    boolean employeeExists(String employeeId);

    boolean departmentExists(String departmentId);
}
