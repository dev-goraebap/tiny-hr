package com.example.tinyhr.vacation.domain.request;

import org.springframework.data.jpa.repository.JpaRepository;

/** 휴가 신청 상세 리포지토리. 식별자는 결재 신청 식별자. Spring Data JPA 가 구현을 생성한다. */
public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, String> {}
