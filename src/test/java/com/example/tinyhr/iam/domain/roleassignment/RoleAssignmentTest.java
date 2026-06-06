package com.example.tinyhr.iam.domain.roleassignment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.tinyhr.iam.domain.IamErrorCode;
import com.example.tinyhr.shared.kernel.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RoleAssignmentTest {

    @Test
    @DisplayName("부여 생성 시 활성(회수 시각 없음) 상태다")
    void create() {
        RoleAssignment assignment = RoleAssignment.create("user-1", "role-1");

        assertThat(assignment.getId()).isNotBlank();
        assertThat(assignment.getUserAccountId()).isEqualTo("user-1");
        assertThat(assignment.getRoleId()).isEqualTo("role-1");
        assertThat(assignment.getAssignedAt()).isNotNull();
        assertThat(assignment.isActive()).isTrue();
    }

    @Test
    @DisplayName("회수하면 비활성이 된다")
    void revoke() {
        RoleAssignment assignment = RoleAssignment.create("user-1", "role-1");

        assignment.revoke();

        assertThat(assignment.isActive()).isFalse();
        assertThat(assignment.getRevokedAt()).isNotNull();
    }

    @Test
    @DisplayName("이미 회수된 부여는 다시 회수할 수 없다")
    void rejectDoubleRevoke() {
        RoleAssignment assignment = RoleAssignment.create("user-1", "role-1");
        assignment.revoke();

        assertThatThrownBy(assignment::revoke)
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(IamErrorCode.ROLE_ASSIGNMENT_ALREADY_REVOKED);
    }
}
