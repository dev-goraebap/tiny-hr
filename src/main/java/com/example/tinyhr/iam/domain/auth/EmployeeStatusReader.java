package com.example.tinyhr.iam.domain.auth;

import java.util.Optional;

/**
 * 로그인 게이트용 사원 재직 상태 조회 포트(읽기 전용).
 *
 * <p>사원 본체는 organization BC 소유다. iam 은 로그인 허용 판정에 필요한 상태값만 읽기 측
 * 프로젝션으로 가져온다(쓰기 경로는 BC 경계를 넘지 않는다).
 */
public interface EmployeeStatusReader {

    Optional<LoginStatus> findStatus(String employeeId);

    /** organization 의 사원 라이프사이클을 로그인 관점에서 본 값. */
    enum LoginStatus {
        INVITED,
        ACTIVE,
        TERMINATED
    }
}
