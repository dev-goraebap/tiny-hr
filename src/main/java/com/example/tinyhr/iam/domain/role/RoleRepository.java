package com.example.tinyhr.iam.domain.role;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** 역할 쓰기 리포지토리. Spring Data JPA 가 구현을 생성한다. */
public interface RoleRepository extends JpaRepository<Role, String> {

    /** 이름 중복 검사(대소문자 무시, I1 방어). */
    Optional<Role> findByNameIgnoreCase(String name);
}
