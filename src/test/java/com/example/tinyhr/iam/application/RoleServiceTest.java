package com.example.tinyhr.iam.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.example.tinyhr.iam.application.dto.CreateRoleRequest;
import com.example.tinyhr.iam.application.dto.UpdateRoleRequest;
import com.example.tinyhr.iam.domain.IamErrorCode;
import com.example.tinyhr.iam.domain.rbac.Permission;
import com.example.tinyhr.iam.domain.role.Role;
import com.example.tinyhr.iam.domain.role.RoleManagerCounter;
import com.example.tinyhr.iam.domain.role.RoleRepository;
import com.example.tinyhr.iam.domain.roleassignment.RoleAssignment;
import com.example.tinyhr.iam.domain.roleassignment.RoleAssignmentRepository;
import com.example.tinyhr.shared.kernel.BusinessException;
import java.util.List;
import java.util.Optional;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RoleServiceTest {

    @Mock
    RoleRepository roleRepository;

    @Mock
    RoleAssignmentRepository roleAssignmentRepository;

    @Mock
    RoleManagerCounter roleManagerCounter;

    @InjectMocks
    RoleService roleService;

    private static void assertBusiness(ThrowingCallable callable, IamErrorCode expected) {
        assertThatThrownBy(callable)
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(expected);
    }

    @Test
    @DisplayName("새 역할을 생성한다")
    void create() {
        given(roleRepository.findByNameIgnoreCase("관리자")).willReturn(Optional.empty());

        String id = roleService.create(
                new CreateRoleRequest("관리자", null, List.of(Permission.ROLE_MANAGE)));

        assertThat(id).isNotBlank();
        then(roleRepository).should().save(any(Role.class));
    }

    @Test
    @DisplayName("이름이 중복되면 생성할 수 없다")
    void rejectDuplicateName() {
        Role existing = Role.create("관리자", null, List.of());
        given(roleRepository.findByNameIgnoreCase("관리자")).willReturn(Optional.of(existing));

        assertBusiness(() -> roleService.create(new CreateRoleRequest("관리자", null, List.of())),
                IamErrorCode.ROLE_NAME_DUPLICATED);
        then(roleRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("없는 역할은 수정할 수 없다")
    void rejectUpdateWhenNotFound() {
        given(roleRepository.findById("none")).willReturn(Optional.empty());

        assertBusiness(() -> roleService.update("none", new UpdateRoleRequest("x", null, null)),
                IamErrorCode.ROLE_NOT_FOUND);
    }

    @Test
    @DisplayName("다른 역할이 쓰는 이름으로는 변경할 수 없다")
    void rejectRenameToDuplicate() {
        Role target = Role.create("역할A", null, List.of());
        Role other = Role.create("역할B", null, List.of());
        given(roleRepository.findById(target.getId())).willReturn(Optional.of(target));
        given(roleRepository.findByNameIgnoreCase("역할B")).willReturn(Optional.of(other));

        assertBusiness(
                () -> roleService.update(target.getId(), new UpdateRoleRequest("역할B", null, null)),
                IamErrorCode.ROLE_NAME_DUPLICATED);
    }

    @Test
    @DisplayName("마지막 ROLE_MANAGE 보유자를 없애는 권한 회수는 거부된다")
    void rejectLockoutOnPermissionChange() {
        Role target = Role.create("관리자", null, List.of(Permission.ROLE_MANAGE));
        given(roleRepository.findById(target.getId())).willReturn(Optional.of(target));
        given(roleManagerCounter.count(any(RoleManagerCounter.Options.class))).willReturn(0L);

        assertBusiness(
                () -> roleService.update(target.getId(),
                        new UpdateRoleRequest(null, null, List.of(Permission.ADMIN_PAGE_CONTROL))),
                IamErrorCode.ROLE_LOCKOUT_FORBIDDEN);
        then(roleRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("아카이브하면 역할 비활성 + 활성 부여를 모두 회수한다")
    void archiveCascadeRevoke() {
        Role target = Role.create("역할", null, List.of(Permission.ADMIN_PAGE_CONTROL));
        RoleAssignment a1 = RoleAssignment.create("user-1", target.getId());
        RoleAssignment a2 = RoleAssignment.create("user-2", target.getId());
        given(roleRepository.findById(target.getId())).willReturn(Optional.of(target));
        given(roleAssignmentRepository.findByRoleIdAndRevokedAtIsNull(target.getId()))
                .willReturn(List.of(a1, a2));

        roleService.archive(target.getId());

        assertThat(target.isActive()).isFalse();
        assertThat(a1.isActive()).isFalse();
        assertThat(a2.isActive()).isFalse();
        then(roleAssignmentRepository).should().saveAll(List.of(a1, a2));
    }

    @Test
    @DisplayName("마지막 ROLE_MANAGE 역할 아카이브는 거부된다")
    void rejectLockoutOnArchive() {
        Role target = Role.create("관리자", null, List.of(Permission.ROLE_MANAGE));
        given(roleRepository.findById(target.getId())).willReturn(Optional.of(target));
        given(roleManagerCounter.count(any(RoleManagerCounter.Options.class))).willReturn(0L);

        assertBusiness(() -> roleService.archive(target.getId()),
                IamErrorCode.ROLE_LOCKOUT_FORBIDDEN);
        assertThat(target.isActive()).isTrue();
    }

    @Test
    @DisplayName("아카이브된 역할을 재활성화한다")
    void reactivate() {
        Role target = Role.create("역할", null, List.of());
        target.archive();
        given(roleRepository.findById(target.getId())).willReturn(Optional.of(target));

        roleService.reactivate(target.getId());

        assertThat(target.isActive()).isTrue();
        then(roleRepository).should().save(target);
    }
}
