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
    @DisplayName("최상위 부서는 depth 0, parentId null 로 생성된다")
    void 최상위_생성() {
        Department root = Department.create(null, "  본부  ");
        assertThat(root.getId()).isNotBlank();
        assertThat(root.getName()).isEqualTo("본부");
        assertThat(root.getParentId()).isNull();
        assertThat(root.getDepth()).isZero();
        assertThat(root.isActive()).isTrue();
    }

    @Test
    @DisplayName("하위 팀은 depth 1, parentId 가 상위로 설정된다")
    void 하위_생성() {
        Department root = Department.create(null, "본부");
        Department team = Department.create(root, "1팀");
        assertThat(team.getDepth()).isEqualTo(1);
        assertThat(team.getParentId()).isEqualTo(root.getId());
    }

    @Test
    @DisplayName("3뎁스(팀 아래)는 DEPARTMENT_DEPTH_EXCEEDED")
    void 깊이초과() {
        Department root = Department.create(null, "본부");
        Department team = Department.create(root, "1팀");
        assertBusiness(() -> Department.create(team, "파트"),
                OrganizationErrorCode.DEPARTMENT_DEPTH_EXCEEDED);
    }

    @Test
    @DisplayName("비활성 상위 아래 생성은 DEPARTMENT_PARENT_INACTIVE")
    void 비활성_상위() {
        Department root = Department.create(null, "본부");
        root.archive();
        assertBusiness(() -> Department.create(root, "1팀"),
                OrganizationErrorCode.DEPARTMENT_PARENT_INACTIVE);
    }

    @Test
    @DisplayName("이름 변경은 trim 된다")
    void 이름변경() {
        Department root = Department.create(null, "본부");
        root.rename("  전략본부  ");
        assertThat(root.getName()).isEqualTo("전략본부");
    }

    @Test
    @DisplayName("아카이브된 부서 수정은 DEPARTMENT_ARCHIVED")
    void 아카이브_후_수정금지() {
        Department root = Department.create(null, "본부");
        root.archive();
        assertBusiness(() -> root.rename("x"), OrganizationErrorCode.DEPARTMENT_ARCHIVED);
        assertBusiness(() -> root.updateResponsibilities("y"),
                OrganizationErrorCode.DEPARTMENT_ARCHIVED);
    }

    @Test
    @DisplayName("아카이브하면 비활성 + archivedAt 기록")
    void 아카이브() {
        Department root = Department.create(null, "본부");
        root.archive();
        assertThat(root.isActive()).isFalse();
        assertThat(root.getArchivedAt()).isNotNull();
    }
}
