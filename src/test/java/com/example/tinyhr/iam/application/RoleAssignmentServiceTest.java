package com.example.tinyhr.iam.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.example.tinyhr.iam.application.dto.AssignRoleRequest;
import com.example.tinyhr.iam.domain.IamErrorCode;
import com.example.tinyhr.iam.domain.rbac.Permission;
import com.example.tinyhr.iam.domain.role.Role;
import com.example.tinyhr.iam.domain.role.RoleManagerCounter;
import com.example.tinyhr.iam.domain.role.RoleRepository;
import com.example.tinyhr.iam.domain.roleassignment.RoleAssignment;
import com.example.tinyhr.iam.domain.roleassignment.RoleAssignmentRepository;
import com.example.tinyhr.iam.domain.useraccount.UserAccountRepository;
import com.example.tinyhr.shared.kernel.BusinessException;
import java.util.Optional;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RoleAssignmentServiceTest {

    @Mock
    UserAccountRepository userAccountRepository;

    @Mock
    RoleRepository roleRepository;

    @Mock
    RoleAssignmentRepository roleAssignmentRepository;

    @Mock
    RoleManagerCounter roleManagerCounter;

    @InjectMocks
    RoleAssignmentService roleAssignmentService;

    private static void assertBusiness(ThrowingCallable callable, IamErrorCode expected) {
        assertThatThrownBy(callable)
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(expected);
    }

    @Test
    @DisplayName("사원에게 활성 역할을 부여한다")
    void assign() {
        Role role = Role.create("역할", null, java.util.List.of());
        given(userAccountRepository.existsById("user-1")).willReturn(true);
        given(roleRepository.findById(role.getId())).willReturn(Optional.of(role));
        given(roleAssignmentRepository
                .findByUserAccountIdAndRoleIdAndRevokedAtIsNull("user-1", role.getId()))
                .willReturn(Optional.empty());

        String id = roleAssignmentService.assign(new AssignRoleRequest("user-1", role.getId()));

        assertThat(id).isNotBlank();
        then(roleAssignmentRepository).should().save(any(RoleAssignment.class));
    }

    @Test
    @DisplayName("없는 사원에게는 부여할 수 없다")
    void rejectWhenUserNotFound() {
        given(userAccountRepository.existsById("none")).willReturn(false);

        assertBusiness(() -> roleAssignmentService.assign(new AssignRoleRequest("none", "role-1")),
                IamErrorCode.ROLE_ASSIGNMENT_USER_NOT_FOUND);
    }

    @Test
    @DisplayName("비활성 역할은 부여할 수 없다")
    void rejectWhenRoleInactive() {
        Role role = Role.create("역할", null, java.util.List.of());
        role.archive();
        given(userAccountRepository.existsById("user-1")).willReturn(true);
        given(roleRepository.findById(role.getId())).willReturn(Optional.of(role));

        assertBusiness(
                () -> roleAssignmentService.assign(new AssignRoleRequest("user-1", role.getId())),
                IamErrorCode.ROLE_INACTIVE);
    }

    @Test
    @DisplayName("이미 부여된 역할은 중복 부여할 수 없다")
    void rejectDuplicate() {
        Role role = Role.create("역할", null, java.util.List.of());
        given(userAccountRepository.existsById("user-1")).willReturn(true);
        given(roleRepository.findById(role.getId())).willReturn(Optional.of(role));
        given(roleAssignmentRepository
                .findByUserAccountIdAndRoleIdAndRevokedAtIsNull("user-1", role.getId()))
                .willReturn(Optional.of(RoleAssignment.create("user-1", role.getId())));

        assertBusiness(
                () -> roleAssignmentService.assign(new AssignRoleRequest("user-1", role.getId())),
                IamErrorCode.ROLE_ASSIGNMENT_DUPLICATED);
        then(roleAssignmentRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("없는 부여는 회수할 수 없다")
    void rejectRevokeWhenNotFound() {
        given(roleAssignmentRepository.findById("none")).willReturn(Optional.empty());

        assertBusiness(() -> roleAssignmentService.revoke("none"),
                IamErrorCode.ROLE_ASSIGNMENT_NOT_FOUND);
    }

    @Test
    @DisplayName("마지막 ROLE_MANAGE 보유자를 없애는 회수는 거부된다")
    void rejectLockoutOnRevoke() {
        Role role = Role.create("관리자", null, java.util.List.of(Permission.ROLE_MANAGE));
        RoleAssignment assignment = RoleAssignment.create("user-1", role.getId());
        given(roleAssignmentRepository.findById(assignment.getId()))
                .willReturn(Optional.of(assignment));
        given(roleRepository.findById(role.getId())).willReturn(Optional.of(role));
        given(roleManagerCounter.count(any(RoleManagerCounter.Options.class))).willReturn(0L);

        assertBusiness(() -> roleAssignmentService.revoke(assignment.getId()),
                IamErrorCode.ROLE_LOCKOUT_FORBIDDEN);
        assertThat(assignment.isActive()).isTrue();
    }

    @Test
    @DisplayName("부여를 회수한다")
    void revoke() {
        Role role = Role.create("역할", null, java.util.List.of(Permission.ADMIN_PAGE_CONTROL));
        RoleAssignment assignment = RoleAssignment.create("user-1", role.getId());
        given(roleAssignmentRepository.findById(assignment.getId()))
                .willReturn(Optional.of(assignment));
        given(roleRepository.findById(role.getId())).willReturn(Optional.of(role));

        roleAssignmentService.revoke(assignment.getId());

        assertThat(assignment.isActive()).isFalse();
        then(roleAssignmentRepository).should().save(assignment);
    }
}
