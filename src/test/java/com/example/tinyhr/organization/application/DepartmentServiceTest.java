package com.example.tinyhr.organization.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.example.tinyhr.organization.application.dto.CreateDepartmentRequest;
import com.example.tinyhr.organization.application.dto.ReorderDepartmentsRequest;
import com.example.tinyhr.organization.application.dto.UpdateDepartmentRequest;
import com.example.tinyhr.organization.domain.OrganizationErrorCode;
import com.example.tinyhr.organization.domain.department.Department;
import com.example.tinyhr.organization.domain.department.DepartmentRepository;
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
class DepartmentServiceTest {

    @Mock
    DepartmentRepository departmentRepository;

    @InjectMocks
    DepartmentService departmentService;

    private static void assertBusiness(ThrowingCallable callable, OrganizationErrorCode expected) {
        assertThatThrownBy(callable)
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(expected);
    }

    @Test
    @DisplayName("지정한 상위 부서가 없으면 등록할 수 없다")
    void rejectWhenParentMissing() {
        // given
        given(departmentRepository.findById("p")).willReturn(Optional.empty());

        // when & then
        assertBusiness(() -> departmentService.create(new CreateDepartmentRequest("1팀", "p")),
                OrganizationErrorCode.DEPARTMENT_PARENT_NOT_FOUND);
    }

    @Test
    @DisplayName("같은 상위에 같은 이름의 부서가 있으면 등록할 수 없다")
    void rejectDuplicateName() {
        // given
        given(departmentRepository.existsInParentByName(eq("본부"), isNull())).willReturn(true);

        // when & then
        assertBusiness(() -> departmentService.create(new CreateDepartmentRequest("본부", null)),
                OrganizationErrorCode.DEPARTMENT_NAME_DUPLICATED);
        then(departmentRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("최상위 부서를 등록한다")
    void register() {
        // given
        given(departmentRepository.existsInParentByName(eq("본부"), isNull())).willReturn(false);

        // when
        String id = departmentService.create(new CreateDepartmentRequest("본부", null));

        // then
        assertThat(id).isNotBlank();
        then(departmentRepository).should().save(any(Department.class));
    }

    @Test
    @DisplayName("없는 부서는 수정할 수 없다")
    void rejectUpdateWhenNotFound() {
        // given
        given(departmentRepository.findById("none")).willReturn(Optional.empty());

        // when & then
        assertBusiness(() -> departmentService.update("none", new UpdateDepartmentRequest("x", null)),
                OrganizationErrorCode.DEPARTMENT_NOT_FOUND);
    }

    @Test
    @DisplayName("같은 상위에 이름이 겹치면 수정할 수 없다")
    void rejectUpdateWithDuplicateName() {
        // given
        Department root = Department.create(null, "본부");
        given(departmentRepository.findById(root.getId())).willReturn(Optional.of(root));
        given(departmentRepository.existsInParentByNameExcludingId(eq("전략"), isNull(), eq(root.getId())))
                .willReturn(true);

        // when & then
        assertBusiness(
                () -> departmentService.update(root.getId(), new UpdateDepartmentRequest("전략", null)),
                OrganizationErrorCode.DEPARTMENT_NAME_DUPLICATED);
    }

    @Test
    @DisplayName("활성 하위 부서가 있으면 아카이브할 수 없다")
    void rejectArchiveWithActiveChildren() {
        // given
        Department root = Department.create(null, "본부");
        Department child = Department.create(root, "1팀");
        given(departmentRepository.findById(root.getId())).willReturn(Optional.of(root));
        given(departmentRepository.findByParentId(root.getId())).willReturn(List.of(child));

        // when & then
        assertBusiness(() -> departmentService.archive(root.getId()),
                OrganizationErrorCode.DEPARTMENT_HAS_ACTIVE_CHILDREN);
        then(departmentRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("활성 하위 부서가 없으면 아카이브한다")
    void archive() {
        // given
        Department root = Department.create(null, "본부");
        given(departmentRepository.findById(root.getId())).willReturn(Optional.of(root));
        given(departmentRepository.findByParentId(root.getId())).willReturn(List.of());

        // when
        departmentService.archive(root.getId());

        // then
        assertThat(root.isActive()).isFalse();
        then(departmentRepository).should().save(root);
    }

    @Test
    @DisplayName("같은 부서를 두 번 지정해 재정렬할 수 없다")
    void rejectReorderWithDuplicateId() {
        // when & then
        assertBusiness(
                () -> departmentService.reorderSiblings(
                        new ReorderDepartmentsRequest(null, List.of("a", "a"))),
                OrganizationErrorCode.DEPARTMENT_REORDER_DUPLICATED);
    }

    @Test
    @DisplayName("재정렬 대상이 지정한 상위에 속하지 않으면 재정렬할 수 없다")
    void rejectReorderWithParentMismatch() {
        // given
        Department root = Department.create(null, "본부");
        Department child = Department.create(root, "1팀");
        given(departmentRepository.findById(child.getId())).willReturn(Optional.of(child));

        // when & then (최상위 재정렬에 하위 팀을 넣음)
        assertBusiness(
                () -> departmentService.reorderSiblings(
                        new ReorderDepartmentsRequest(null, List.of(child.getId()))),
                OrganizationErrorCode.DEPARTMENT_REORDER_PARENT_MISMATCH);
    }

    @Test
    @DisplayName("지정한 순서대로 형제 부서 순서가 매겨진다")
    void reorder() {
        // given
        Department a = Department.create(null, "a");
        Department b = Department.create(null, "b");
        given(departmentRepository.findById(a.getId())).willReturn(Optional.of(a));
        given(departmentRepository.findById(b.getId())).willReturn(Optional.of(b));

        // when (b 를 먼저)
        departmentService.reorderSiblings(
                new ReorderDepartmentsRequest(null, List.of(b.getId(), a.getId())));

        // then
        assertThat(b.getDisplayOrder()).isEqualTo(1);
        assertThat(a.getDisplayOrder()).isEqualTo(2);
        then(departmentRepository).should().saveAll(any());
    }
}
