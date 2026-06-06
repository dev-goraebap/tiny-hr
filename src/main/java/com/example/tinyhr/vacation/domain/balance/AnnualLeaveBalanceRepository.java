package com.example.tinyhr.vacation.domain.balance;

import org.springframework.data.jpa.repository.JpaRepository;

/** 연차 잔액 리포지토리. 식별자는 employeeId. Spring Data JPA 가 구현을 생성한다. */
public interface AnnualLeaveBalanceRepository extends JpaRepository<AnnualLeaveBalance, String> {}
