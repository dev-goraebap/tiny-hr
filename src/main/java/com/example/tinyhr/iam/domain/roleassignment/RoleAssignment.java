package com.example.tinyhr.iam.domain.roleassignment;

import com.example.tinyhr.iam.domain.IamErrorCode;
import com.example.tinyhr.shared.kernel.BusinessException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 사원(UserAccount) ↔ 역할(Role) 부여 관계 애그리거트.
 *
 * <p>회수(revoke) = 소프트 삭제. {@code revokedAt} 한 필드로 활성/회수를 표현하고,
 * 권한 판정({@code RbacOpenHostService.has})은 {@code 부여 active ∧ role active} 규칙을 쓴다.
 * 한 사원에게 같은 역할은 활성으로 1건만 존재한다(서비스에서 방어).
 */
@Entity
@Table(name = "role_assignment")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RoleAssignment {

    @Id
    @Column(name = "assignment_id", length = 36)
    private String id;

    @Column(name = "user_account_id", nullable = false, length = 36)
    private String userAccountId;

    @Column(name = "role_id", nullable = false, length = 36)
    private String roleId;

    @Column(name = "assigned_at", nullable = false)
    private Instant assignedAt;

    /** 회수 시각. null = 활성. */
    @Column(name = "revoked_at")
    private Instant revokedAt;

    /** 새 부여 생성. 식별자는 도메인이 발급한다. */
    public static RoleAssignment create(String userAccountId, String roleId) {
        RoleAssignment assignment = new RoleAssignment();
        assignment.id = UUID.randomUUID().toString();
        assignment.userAccountId = userAccountId;
        assignment.roleId = roleId;
        assignment.assignedAt = Instant.now();
        assignment.revokedAt = null;
        return assignment;
    }

    /** 회수(소프트 삭제). 멱등이 아니다 — 이미 회수된 부여를 다시 회수하면 도메인 에러. */
    public void revoke() {
        if (revokedAt != null) {
            throw new BusinessException(IamErrorCode.ROLE_ASSIGNMENT_ALREADY_REVOKED);
        }
        this.revokedAt = Instant.now();
    }

    public boolean isActive() {
        return revokedAt == null;
    }
}
