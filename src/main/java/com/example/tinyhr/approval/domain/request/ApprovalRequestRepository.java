package com.example.tinyhr.approval.domain.request;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 결재 요청 리포지토리. 쓰기 워크플로(제출·결재·취소)는 애그리거트를 통째로 로드·저장한다.
 * Spring Data JPA 가 구현을 생성한다.
 */
public interface ApprovalRequestRepository extends JpaRepository<ApprovalRequest, String> {}
