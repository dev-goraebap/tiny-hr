package com.example.tinyhr.organization.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
    @DisplayName("상위 부서가 없으면 DEPARTMENT_PARENT_NOT_FOUND")
    void 등록_상위없음() {
        when(departmentRepository.findById("p")).thenReturn(Optional.empty());
        assertBusiness(() -> departmentService.create(new CreateDepartmentRequest("1팀", "p")),
                OrganizationErrorCode.DEPARTMENT_PARENT_NOT_FOUND);
    }

    @Test
    @DisplayName("같은 상위에 같은 이름이면 DEPARTMENT_NAME_DUPLICATED")
    void 등록_이름중복() {
        when(departmentRepository.existsInParentByName(eq("본부"), isNull())).thenReturn(true);
        assertBusiness(() -> departmentService.create(new CreateDepartmentRequest("본부", null)),
                OrganizationErrorCode.DEPARTMENT_NAME_DUPLICATED);
        verify(departmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("최상위 부서를 등록하면 식별자를 반환하고 저장한다")
    void 등록_성공() {
        when(departmentRepository.existsInParentByName(eq("본부"), isNull())).thenReturn(false);

        String id = departmentService.create(new CreateDepartmentRequest("본부", null));

        assertThat(id).isNotBlank();
        verify(departmentRepository).save(any(Department.class));
    }

    @Test
    @DisplayName("없는 부서 수정은 DEPARTMENT_NOT_FOUND")
    void 수정_없음() {
        when(departmentRepository.findById("none")).thenReturn(Optional.empty());
        assertBusiness(() -> departmentService.update("none", new UpdateDepartmentRequest("x", null)),
                OrganizationErrorCode.DEPARTMENT_NOT_FOUND);
    }

    @Test
    @DisplayName("같은 상위에 이름이 겹치면 DEPARTMENT_NAME_DUPLICATED")
    void 수정_이름중복() {
        Department root = Department.create(null, "본부");
        when(departmentRepository.findById(root.getId())).thenReturn(Optional.of(root));
        when(departmentRepository.existsInParentByNameExcludingId(eq("전략"), isNull(), eq(root.getId())))
                .thenReturn(true);

        assertBusiness(
                () -> departmentService.update(root.getId(), new UpdateDepartmentRequest("전략", null)),
                OrganizationErrorCode.DEPARTMENT_NAME_DUPLICATED);
    }

    @Test
    @DisplayName("활성 하위 부서가 있으면 아카이브 거부 DEPARTMENT_HAS_ACTIVE_CHILDREN")
    void 아카이브_활성하위() {
        Department root = Department.create(null, "본부");
        Department child = Department.create(root, "1팀");
        when(departmentRepository.findById(root.getId())).thenReturn(Optional.of(root));
        when(departmentRepository.findByParentId(root.getId())).thenReturn(List.of(child));

        assertBusiness(() -> departmentService.archive(root.getId()),
                OrganizationErrorCode.DEPARTMENT_HAS_ACTIVE_CHILDREN);
        verify(departmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("활성 하위가 없으면 아카이브된다")
    void 아카이브_성공() {
        Department root = Department.create(null, "본부");
        when(departmentRepository.findById(root.getId())).thenReturn(Optional.of(root));
        when(departmentRepository.findByParentId(root.getId())).thenReturn(List.of());

        departmentService.archive(root.getId());

        assertThat(root.isActive()).isFalse();
        verify(departmentRepository).save(root);
    }

    @Test
    @DisplayName("재정렬 입력에 같은 ID 중복이면 DEPARTMENT_REORDER_DUPLICATED")
    void 재정렬_중복() {
        assertBusiness(
                () -> departmentService.reorderSiblings(
                        new ReorderDepartmentsRequest(null, List.of("a", "a"))),
                OrganizationErrorCode.DEPARTMENT_REORDER_DUPLICATED);
    }

    @Test
    @DisplayName("재정렬 대상의 상위가 다르면 DEPARTMENT_REORDER_PARENT_MISMATCH")
    void 재정렬_상위불일치() {
        Department root = Department.create(null, "본부");
        Department child = Department.create(root, "1팀");
        when(departmentRepository.findById(child.getId())).thenReturn(Optional.of(child));

        // 최상위(parentId=null) 재정렬에 하위 팀(child.parentId=root)을 넣음
        assertBusiness(
                () -> departmentService.reorderSiblings(
                        new ReorderDepartmentsRequest(null, List.of(child.getId()))),
                OrganizationErrorCode.DEPARTMENT_REORDER_PARENT_MISMATCH);
    }

    @Test
    @DisplayName("재정렬하면 입력 순서대로 displayOrder 가 1부터 매겨진다")
    void 재정렬_성공() {
        Department a = Department.create(null, "a");
        Department b = Department.create(null, "b");
        when(departmentRepository.findById(a.getId())).thenReturn(Optional.of(a));
        when(departmentRepository.findById(b.getId())).thenReturn(Optional.of(b));

        departmentService.reorderSiblings(
                new ReorderDepartmentsRequest(null, List.of(b.getId(), a.getId())));

        assertThat(b.getDisplayOrder()).isEqualTo(1);
        assertThat(a.getDisplayOrder()).isEqualTo(2);
        verify(departmentRepository).saveAll(any());
    }
}
