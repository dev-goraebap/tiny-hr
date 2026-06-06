package com.example.tinyhr.iam.domain.roleassignment;

import com.example.tinyhr.iam.domain.rbac.Permission;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** 역할 부여 쓰기 리포지토리. Spring Data JPA 가 구현을 생성한다. */
public interface RoleAssignmentRepository extends JpaRepository<RoleAssignment, String> {

    /** 사원의 활성 부여만 — 권한 계산 핫패스. */
    List<RoleAssignment> findByUserAccountIdAndRevokedAtIsNull(String userAccountId);

    /** 특정 역할의 활성 부여 — 아카이브 시 cascade revoke 용. */
    List<RoleAssignment> findByRoleIdAndRevokedAtIsNull(String roleId);

    /** 같은 사원·역할의 활성 부여(중복 방어). */
    Optional<RoleAssignment> findByUserAccountIdAndRoleIdAndRevokedAtIsNull(
            String userAccountId, String roleId);

    /**
     * self-lockout 시뮬레이션 카운트 — 활성 부여 × 활성 역할 × 해당 권한 보유를 만족하는 사원 수.
     * 제외 목록은 비어 있으면 안 되므로(센티넬 보장) 어댑터가 채워 넣는다.
     */
    @Query("""
            select count(distinct ra.userAccountId)
            from RoleAssignment ra, Role r join r.permissions p
            where r.id = ra.roleId
              and ra.revokedAt is null
              and r.active = true
              and p = :permission
              and ra.id not in :excludeAssignmentIds
              and r.id not in :excludeRoleIds
            """)
    long countEffectiveHolders(
            @Param("permission") Permission permission,
            @Param("excludeAssignmentIds") Collection<String> excludeAssignmentIds,
            @Param("excludeRoleIds") Collection<String> excludeRoleIds);
}
