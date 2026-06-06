package com.example.tinyhr.organization.domain.department;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.tinyhr.organization.domain.OrganizationErrorCode;
import com.example.tinyhr.shared.kernel.BusinessException;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DepartmentTest {

    private static void assertBusiness(ThrowingCallable callable, OrganizationErrorCode expected) {
        assertThatThrownBy(callable)
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(expected);
    }

    @Test
    @DisplayName("최상위 부서는 상위 없이 만들어진다")
    void createRoot() {
        // when
        Department root = Department.create(null, "  본부  ");

        // then
        assertThat(root.getId()).isNotBlank();
        assertThat(root.getName()).isEqualTo("본부");
        assertThat(root.getParentId()).isNull();
        assertThat(root.getDepth()).isZero();
        assertThat(root.isActive()).isTrue();
    }

    @Test
    @DisplayName("하위 팀은 상위 부서 아래에 만들어진다")
    void createChild() {
        // given
        Department root = Department.create(null, "본부");

        // when
        Department team = Department.create(root, "1팀");

        // then
        assertThat(team.getDepth()).isEqualTo(1);
        assertThat(team.getParentId()).isEqualTo(root.getId());
    }

    @Test
    @DisplayName("부서 트리는 부서·팀 2단계까지만 만들 수 있다")
    void rejectThirdLevel() {
        // given
        Department root = Department.create(null, "본부");
        Department team = Department.create(root, "1팀");

        // when & then
        assertBusiness(() -> Department.create(team, "파트"),
                OrganizationErrorCode.DEPARTMENT_DEPTH_EXCEEDED);
    }

    @Test
    @DisplayName("아카이브된 상위 부서 아래에는 만들 수 없다")
    void rejectInactiveParent() {
        // given
        Department root = Department.create(null, "본부");
        root.archive();

        // when & then
        assertBusiness(() -> Department.create(root, "1팀"),
                OrganizationErrorCode.DEPARTMENT_PARENT_INACTIVE);
    }

    @Test
    @DisplayName("부서 이름을 바꿀 수 있다")
    void rename() {
        // given
        Department root = Department.create(null, "본부");

        // when
        root.rename("  전략본부  ");

        // then
        assertThat(root.getName()).isEqualTo("전략본부");
    }

    @Test
    @DisplayName("아카이브된 부서는 수정할 수 없다")
    void rejectModifyAfterArchive() {
        // given
        Department root = Department.create(null, "본부");
        root.archive();

        // when & then
        assertBusiness(() -> root.rename("x"), OrganizationErrorCode.DEPARTMENT_ARCHIVED);
        assertBusiness(() -> root.updateResponsibilities("y"),
                OrganizationErrorCode.DEPARTMENT_ARCHIVED);
    }

    @Test
    @DisplayName("부서를 아카이브하면 비활성이 된다")
    void archive() {
        // given
        Department root = Department.create(null, "본부");

        // when
        root.archive();

        // then
        assertThat(root.isActive()).isFalse();
        assertThat(root.getArchivedAt()).isNotNull();
    }
}
