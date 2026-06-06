package com.example.tinyhr.approval.application;

import com.example.tinyhr.approval.application.dto.ApprovalTemplateApproverRequest;
import com.example.tinyhr.approval.application.dto.CreateApprovalTemplateRequest;
import com.example.tinyhr.approval.application.dto.UpdateApprovalTemplateRequest;
import com.example.tinyhr.approval.domain.ApprovalErrorCode;
import com.example.tinyhr.approval.domain.template.ApprovalTemplate;
import com.example.tinyhr.approval.domain.template.ApprovalTemplate.ApproverInput;
import com.example.tinyhr.approval.domain.template.ApprovalTemplateRepository;
import com.example.tinyhr.approval.domain.template.OrgDirectoryReadRepository;
import com.example.tinyhr.shared.kernel.BusinessException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 결재선 템플릿(결재선 카탈로그)을 생성·수정·삭제한다.
 *
 * @actor 관리자
 */
@Service
@Transactional
public class ApprovalTemplateService {

    private final ApprovalTemplateRepository approvalTemplateRepository;
    private final OrgDirectoryReadRepository orgDirectoryReadRepository;

    public ApprovalTemplateService(
            ApprovalTemplateRepository approvalTemplateRepository,
            OrgDirectoryReadRepository orgDirectoryReadRepository) {
        this.approvalTemplateRepository = approvalTemplateRepository;
        this.orgDirectoryReadRepository = orgDirectoryReadRepository;
    }

    /** 결재선 생성. 부서·결재자 존재와 (부서,카테고리) 유일성을 검증한다. */
    public String create(CreateApprovalTemplateRequest request) {
        if (!orgDirectoryReadRepository.departmentExists(request.departmentId())) {
            throw new BusinessException(ApprovalErrorCode.APPROVAL_TEMPLATE_DEPARTMENT_NOT_FOUND);
        }
        assertEmployeesExist(request.approvers());
        approvalTemplateRepository
                .findByDepartmentIdAndCategory(request.departmentId(), request.category())
                .ifPresent(dup -> {
                    throw new BusinessException(
                            ApprovalErrorCode.APPROVAL_TEMPLATE_DUPLICATE_CATEGORY);
                });

        ApprovalTemplate template = ApprovalTemplate.create(
                request.departmentId(), request.category(), toInputs(request.approvers()));
        approvalTemplateRepository.save(template);
        return template.getId();
    }

    /** 결재자 구성을 교체한다. */
    public void update(String templateId, UpdateApprovalTemplateRequest request) {
        ApprovalTemplate template = approvalTemplateRepository.findById(templateId)
                .orElseThrow(() ->
                        new BusinessException(ApprovalErrorCode.APPROVAL_TEMPLATE_NOT_FOUND));
        assertEmployeesExist(request.approvers());
        template.replaceApprovers(toInputs(request.approvers()));
        approvalTemplateRepository.save(template);
    }

    /** 결재선을 삭제한다(물리 삭제 — 자식 결재자 포함). */
    public void delete(String templateId) {
        ApprovalTemplate template = approvalTemplateRepository.findById(templateId)
                .orElseThrow(() ->
                        new BusinessException(ApprovalErrorCode.APPROVAL_TEMPLATE_NOT_FOUND));
        approvalTemplateRepository.delete(template);
    }

    private void assertEmployeesExist(List<ApprovalTemplateApproverRequest> approvers) {
        approvers.stream().map(ApprovalTemplateApproverRequest::employeeId).distinct()
                .forEach(employeeId -> {
                    if (!orgDirectoryReadRepository.employeeExists(employeeId)) {
                        throw new BusinessException(
                                ApprovalErrorCode.APPROVAL_TEMPLATE_EMPLOYEE_NOT_FOUND);
                    }
                });
    }

    private static List<ApproverInput> toInputs(List<ApprovalTemplateApproverRequest> approvers) {
        return approvers.stream()
                .map(a -> new ApproverInput(a.employeeId(), a.orderNo()))
                .toList();
    }
}
