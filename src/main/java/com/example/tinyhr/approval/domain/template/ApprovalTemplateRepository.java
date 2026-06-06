package com.example.tinyhr.approval.domain.template;

import com.example.tinyhr.approval.domain.ApprovalLineCategory;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** 결재선 템플릿 리포지토리. Spring Data JPA 가 구현을 생성한다. */
public interface ApprovalTemplateRepository extends JpaRepository<ApprovalTemplate, String> {

    /** (부서, 카테고리) 유일성 검사용. */
    Optional<ApprovalTemplate> findByDepartmentIdAndCategory(
            String departmentId, ApprovalLineCategory category);
}
