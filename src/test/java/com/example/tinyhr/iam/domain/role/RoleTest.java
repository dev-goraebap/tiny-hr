package com.example.tinyhr.iam.domain.role;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.tinyhr.iam.domain.IamErrorCode;
import com.example.tinyhr.iam.domain.rbac.Permission;
import com.example.tinyhr.shared.kernel.BusinessException;
import java.util.List;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RoleTest {

    private static void assertBusiness(ThrowingCallable callable, IamErrorCode expected) {
        assertThatThrownBy(callable)
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(expected);
    }

    @Test
    @DisplayName("역할 생성 시 이름 trim·활성, 권한은 중복 제거된다")
    void create() {
        Role role = Role.create("  관리자  ", "  설명  ",
                List.of(Permission.ROLE_MANAGE, Permission.ROLE_MANAGE, Permission.ADMIN_PAGE_CONTROL));

        assertThat(role.getId()).isNotBlank();
        assertThat(role.getName()).isEqualTo("관리자");
        assertThat(role.getDescription()).isEqualTo("설명");
        assertThat(role.isActive()).isTrue();
        assertThat(role.isSystem()).isFalse();
        assertThat(role.getPermissions())
                .containsExactlyInAnyOrder(Permission.ROLE_MANAGE, Permission.ADMIN_PAGE_CONTROL);
    }

    @Test
    @DisplayName("이름이 공백뿐이면 생성할 수 없다")
    void rejectBlankName() {
        assertBusiness(() -> Role.create("   ", null, List.of()), IamErrorCode.ROLE_NAME_INVALID);
    }

    @Test
    @DisplayName("이름·설명·권한을 변경할 수 있다")
    void edit() {
        Role role = Role.create("역할", null, List.of(Permission.ADMIN_PAGE_CONTROL));

        role.rename("새이름");
        role.updateDescription("새설명");
        role.changePermissions(List.of(Permission.OVERTIME_CONTROL));

        assertThat(role.getName()).isEqualTo("새이름");
        assertThat(role.getDescription()).isEqualTo("새설명");
        assertThat(role.getPermissions()).containsExactly(Permission.OVERTIME_CONTROL);
    }

    @Test
    @DisplayName("시스템 역할은 이름 변경·아카이브가 금지된다")
    void systemRoleProtected() {
        Role role = Role.createSystem("시스템", null, List.of(Permission.ROLE_MANAGE));

        assertBusiness(() -> role.rename("x"), IamErrorCode.ROLE_SYSTEM_RENAME_FORBIDDEN);
        assertBusiness(role::archive, IamErrorCode.ROLE_SYSTEM_ARCHIVE_FORBIDDEN);
    }

    @Test
    @DisplayName("아카이브하면 비활성, 재활성화하면 활성으로 돌아온다")
    void archiveAndReactivate() {
        Role role = Role.create("역할", null, List.of());

        role.archive();
        assertThat(role.isActive()).isFalse();

        role.reactivate();
        assertThat(role.isActive()).isTrue();
    }

    @Test
    @DisplayName("비활성 역할은 속성을 편집할 수 없다")
    void inactiveNotEditable() {
        Role role = Role.create("역할", null, List.of());
        role.archive();

        assertBusiness(() -> role.rename("x"), IamErrorCode.ROLE_INACTIVE);
        assertBusiness(() -> role.changePermissions(List.of()), IamErrorCode.ROLE_INACTIVE);
    }

    @Test
    @DisplayName("hasPermission 으로 보유 여부를 확인한다")
    void hasPermission() {
        Role role = Role.create("역할", null, List.of(Permission.ROLE_MANAGE));

        assertThat(role.hasPermission(Permission.ROLE_MANAGE)).isTrue();
        assertThat(role.hasPermission(Permission.OVERTIME_CONTROL)).isFalse();
    }
}
