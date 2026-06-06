package com.example.tinyhr.approval.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.example.tinyhr.approval.application.dto.ApprovalTemplateApproverRequest;
import com.example.tinyhr.approval.application.dto.CreateApprovalTemplateRequest;
import com.example.tinyhr.approval.application.dto.UpdateApprovalTemplateRequest;
import com.example.tinyhr.approval.domain.ApprovalErrorCode;
import com.example.tinyhr.approval.domain.ApprovalLineCategory;
import com.example.tinyhr.approval.domain.template.ApprovalTemplate;
import com.example.tinyhr.approval.domain.template.ApprovalTemplateRepository;
import com.example.tinyhr.approval.domain.template.OrgDirectoryReadRepository;
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
class ApprovalTemplateServiceTest {

    @Mock ApprovalTemplateRepository approvalTemplateRepository;
    @Mock OrgDirectoryReadRepository orgDirectoryReadRepository;

    @InjectMocks ApprovalTemplateService approvalTemplateService;

    private static CreateApprovalTemplateRequest createReq() {
        return new CreateApprovalTemplateRequest("dept-1", ApprovalLineCategory.ANNUAL,
                List.of(new ApprovalTemplateApproverRequest("e1", 1)));
    }

    private static void assertBusiness(ThrowingCallable callable, ApprovalErrorCode expected) {
        assertThatThrownBy(callable)
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(expected);
    }

    @Test
    @DisplayName("부서가 없으면 생성할 수 없다")
    void create_departmentNotFound() {
        given(orgDirectoryReadRepository.departmentExists("dept-1")).willReturn(false);

        assertBusiness(() -> approvalTemplateService.create(createReq()),
                ApprovalErrorCode.APPROVAL_TEMPLATE_DEPARTMENT_NOT_FOUND);
    }

    @Test
    @DisplayName("결재자 사원이 없으면 생성할 수 없다")
    void create_employeeNotFound() {
        given(orgDirectoryReadRepository.departmentExists("dept-1")).willReturn(true);
        given(orgDirectoryReadRepository.employeeExists("e1")).willReturn(false);

        assertBusiness(() -> approvalTemplateService.create(createReq()),
                ApprovalErrorCode.APPROVAL_TEMPLATE_EMPLOYEE_NOT_FOUND);
    }

    @Test
    @DisplayName("같은 부서·카테고리가 이미 있으면 생성할 수 없다")
    void create_duplicateCategory() {
        given(orgDirectoryReadRepository.departmentExists("dept-1")).willReturn(true);
        given(orgDirectoryReadRepository.employeeExists("e1")).willReturn(true);
        given(approvalTemplateRepository.findByDepartmentIdAndCategory(
                "dept-1", ApprovalLineCategory.ANNUAL))
                .willReturn(Optional.of(ApprovalTemplate.create("dept-1",
                        ApprovalLineCategory.ANNUAL,
                        List.of(new ApprovalTemplate.ApproverInput("x", 1)))));

        assertBusiness(() -> approvalTemplateService.create(createReq()),
                ApprovalErrorCode.APPROVAL_TEMPLATE_DUPLICATE_CATEGORY);
        then(approvalTemplateRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("결재선을 생성한다")
    void create() {
        given(orgDirectoryReadRepository.departmentExists("dept-1")).willReturn(true);
        given(orgDirectoryReadRepository.employeeExists("e1")).willReturn(true);
        given(approvalTemplateRepository.findByDepartmentIdAndCategory(
                "dept-1", ApprovalLineCategory.ANNUAL)).willReturn(Optional.empty());

        String id = approvalTemplateService.create(createReq());

        assertThat(id).isNotBlank();
        then(approvalTemplateRepository).should().save(any(ApprovalTemplate.class));
    }

    @Test
    @DisplayName("없는 결재선은 수정할 수 없다")
    void update_notFound() {
        given(approvalTemplateRepository.findById("none")).willReturn(Optional.empty());

        assertBusiness(() -> approvalTemplateService.update("none",
                        new UpdateApprovalTemplateRequest(
                                List.of(new ApprovalTemplateApproverRequest("e1", 1)))),
                ApprovalErrorCode.APPROVAL_TEMPLATE_NOT_FOUND);
    }

    @Test
    @DisplayName("없는 결재선은 삭제할 수 없다")
    void delete_notFound() {
        given(approvalTemplateRepository.findById("none")).willReturn(Optional.empty());

        assertBusiness(() -> approvalTemplateService.delete("none"),
                ApprovalErrorCode.APPROVAL_TEMPLATE_NOT_FOUND);
    }

    @Test
    @DisplayName("결재자 사원 검증은 중복 없이 수행한다")
    void update_validatesEmployees() {
        ApprovalTemplate existing = ApprovalTemplate.create("dept-1", ApprovalLineCategory.ANNUAL,
                List.of(new ApprovalTemplate.ApproverInput("old", 1)));
        given(approvalTemplateRepository.findById(existing.getId()))
                .willReturn(Optional.of(existing));
        given(orgDirectoryReadRepository.employeeExists(anyString())).willReturn(true);

        approvalTemplateService.update(existing.getId(),
                new UpdateApprovalTemplateRequest(
                        List.of(new ApprovalTemplateApproverRequest("e9", 1))));

        then(approvalTemplateRepository).should().save(existing);
        assertThat(existing.getApprovers()).hasSize(1);
    }
}
