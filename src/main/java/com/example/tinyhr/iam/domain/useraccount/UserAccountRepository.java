package com.example.tinyhr.iam.domain.useraccount;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** 인증 계정 리포지토리. Spring Data JPA 가 구현을 생성한다. */
public interface UserAccountRepository extends JpaRepository<UserAccount, String> {

    Optional<UserAccount> findByEmail(String email);
}
