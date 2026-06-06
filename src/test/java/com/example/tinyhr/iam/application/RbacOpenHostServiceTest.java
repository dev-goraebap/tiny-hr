package com.example.tinyhr.iam.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.example.tinyhr.iam.domain.rbac.Permission;
import com.example.tinyhr.iam.domain.role.Role;
import com.example.tinyhr.iam.domain.role.RoleRepository;
import com.example.tinyhr.iam.domain.roleassignment.RoleAssignment;
import com.example.tinyhr.iam.domain.roleassignment.RoleAssignmentRepository;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RbacOpenHostServiceTest {

    @Mock
    RoleAssignmentRepository roleAssignmentRepository;

    @Mock
    RoleRepository roleRepository;

    @InjectMocks
    RbacOpenHostService rbacOpenHostService;

    @Test
    @DisplayName("부여가 없으면 어떤 권한도 보유하지 않는다")
    void hasFalseWhenNoAssignment() {
        given(roleAssignmentRepository.findByUserAccountIdAndRevokedAtIsNull("user-1"))
                .willReturn(List.of());

        assertThat(rbacOpenHostService.has("user-1", Permission.ROLE_MANAGE)).isFalse();
    }

    @Test
    @DisplayName("활성 역할에 해당 권한이 있으면 보유로 판정한다")
    void hasTrue() {
        Role role = Role.create("관리자", null, List.of(Permission.ROLE_MANAGE));
        given(roleAssignmentRepository.findByUserAccountIdAndRevokedAtIsNull("user-1"))
                .willReturn(List.of(RoleAssignment.create("user-1", role.getId())));
        given(roleRepository.findAllById(List.of(role.getId()))).willReturn(List.of(role));

        assertThat(rbacOpenHostService.has("user-1", Permission.ROLE_MANAGE)).isTrue();
        assertThat(rbacOpenHostService.has("user-1", Permission.OVERTIME_CONTROL)).isFalse();
    }

    @Test
    @DisplayName("비활성 역할의 권한은 무시한다")
    void ignoreInactiveRole() {
        Role role = Role.create("관리자", null, List.of(Permission.ROLE_MANAGE));
        role.archive();
        given(roleAssignmentRepository.findByUserAccountIdAndRevokedAtIsNull("user-1"))
                .willReturn(List.of(RoleAssignment.create("user-1", role.getId())));
        given(roleRepository.findAllById(List.of(role.getId()))).willReturn(List.of(role));

        assertThat(rbacOpenHostService.has("user-1", Permission.ROLE_MANAGE)).isFalse();
    }

    @Test
    @DisplayName("유효 권한은 활성 역할들의 합집합이다")
    void listEffectiveUnion() {
        Role a = Role.create("A", null, List.of(Permission.ROLE_MANAGE, Permission.ADMIN_PAGE_CONTROL));
        Role b = Role.create("B", null, List.of(Permission.ADMIN_PAGE_CONTROL, Permission.OVERTIME_CONTROL));
        given(roleAssignmentRepository.findByUserAccountIdAndRevokedAtIsNull("user-1"))
                .willReturn(List.of(
                        RoleAssignment.create("user-1", a.getId()),
                        RoleAssignment.create("user-1", b.getId())));
        given(roleRepository.findAllById(List.of(a.getId(), b.getId()))).willReturn(List.of(a, b));

        assertThat(rbacOpenHostService.listEffective("user-1"))
                .containsExactlyInAnyOrder(
                        Permission.ROLE_MANAGE,
                        Permission.ADMIN_PAGE_CONTROL,
                        Permission.OVERTIME_CONTROL);
    }
}
