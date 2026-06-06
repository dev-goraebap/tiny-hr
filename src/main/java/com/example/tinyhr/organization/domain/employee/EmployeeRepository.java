package com.example.tinyhr.organization.domain.employee;

import java.util.Collection;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 사원 쓰기 리포지토리. Spring Data JPA 가 구현을 생성하므로 별도 구현체를 두지 않는다.
 * 목록·상세 조회는 MyBatis(EmployeeQueryMapper)가 담당한다.
 */
public interface EmployeeRepository extends JpaRepository<Employee, String> {

    /** 재초대 가능 여부 판단용 — ACTIVE/INVITED 상태로 같은 업무 이메일이 이미 있는지. */
    boolean existsByWorkEmailAndStatusIn(String workEmail, Collection<EmployeeStatus> statuses);
}
