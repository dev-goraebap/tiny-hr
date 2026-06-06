package com.example.tinyhr.approval.adapter.web;

import com.example.tinyhr.approval.adapter.mapper.ApprovalTemplateQueryMapper;
import com.example.tinyhr.approval.adapter.mapper.viewmodel.ApprovalTemplateView;
import com.example.tinyhr.approval.application.ApprovalTemplateService;
import com.example.tinyhr.approval.application.dto.CreateApprovalTemplateRequest;
import com.example.tinyhr.approval.application.dto.UpdateApprovalTemplateRequest;
import com.example.tinyhr.approval.domain.ApprovalErrorCode;
import com.example.tinyhr.shared.kernel.ApiResponse;
import com.example.tinyhr.shared.kernel.BusinessException;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 결재선 템플릿 Admin CRUD. {@code /admin/**} 이라 ADMIN_PAGE_CONTROL 권한 필요(SecurityConfig).
 */
@RestController
@RequestMapping("/admin/approval-templates")
public class ApprovalTemplateController {

    private final ApprovalTemplateService approvalTemplateService;
    private final ApprovalTemplateQueryMapper approvalTemplateQueryMapper;

    public ApprovalTemplateController(
            ApprovalTemplateService approvalTemplateService,
            ApprovalTemplateQueryMapper approvalTemplateQueryMapper) {
        this.approvalTemplateService = approvalTemplateService;
        this.approvalTemplateQueryMapper = approvalTemplateQueryMapper;
    }

    @GetMapping
    public ApiResponse<List<ApprovalTemplateView>> list(
            @RequestParam(required = false) String departmentId) {
        List<ApprovalTemplateView> items = departmentId != null
                ? approvalTemplateQueryMapper.listByDepartment(departmentId)
                : approvalTemplateQueryMapper.listAll();
        return ApiResponse.of(items);
    }

    @GetMapping("/{id}")
    public ApiResponse<ApprovalTemplateView> findById(@PathVariable String id) {
        ApprovalTemplateView view = approvalTemplateQueryMapper.findById(id);
        if (view == null) {
            throw new BusinessException(ApprovalErrorCode.APPROVAL_TEMPLATE_NOT_FOUND);
        }
        return ApiResponse.of(view);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void create(@Valid @RequestBody CreateApprovalTemplateRequest request) {
        approvalTemplateService.create(request);
    }

    @PatchMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void update(
            @PathVariable String id, @Valid @RequestBody UpdateApprovalTemplateRequest request) {
        approvalTemplateService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id) {
        approvalTemplateService.delete(id);
    }
}
