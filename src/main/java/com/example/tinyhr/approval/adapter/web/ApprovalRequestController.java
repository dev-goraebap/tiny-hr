package com.example.tinyhr.approval.adapter.web;

import com.example.tinyhr.approval.application.ApprovalService;
import com.example.tinyhr.approval.application.dto.AdminCancelRequest;
import com.example.tinyhr.approval.application.dto.DecideApprovalRequest;
import com.example.tinyhr.iam.adapter.security.AuthPrincipal;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 결재 진행(승인·반려·취소·회수) HTTP 진입점. 결재 종류 무관.
 *
 * <p>{@code /approval/**} 는 인증만 필요(서비스가 결재자·신청자 본인 여부 검증). 관리자 강제 취소는
 * {@code /admin/approval/**} 로 ADMIN_PAGE_CONTROL 권한이 요구된다(SecurityConfig).
 */
@RestController
public class ApprovalRequestController {

    private final ApprovalService approvalService;

    public ApprovalRequestController(ApprovalService approvalService) {
        this.approvalService = approvalService;
    }

    @PostMapping("/approval/requests/{id}/decisions")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void decide(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable String id,
            @Valid @RequestBody DecideApprovalRequest request) {
        approvalService.decide(
                id, principal.userAccountId(), request.action(),
                request.comment(), request.reason());
    }

    @PatchMapping("/approval/requests/{id}/cancel")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(@AuthenticationPrincipal AuthPrincipal principal, @PathVariable String id) {
        approvalService.cancel(id, principal.userAccountId());
    }

    @PatchMapping("/approval/requests/{id}/withdraw")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void withdraw(
            @AuthenticationPrincipal AuthPrincipal principal, @PathVariable String id) {
        approvalService.withdraw(id, principal.userAccountId());
    }

    @PatchMapping("/admin/approval/requests/{id}/cancel")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelAsAdmin(
            @PathVariable String id, @Valid @RequestBody AdminCancelRequest request) {
        approvalService.cancelAsAdmin(id, request.reason());
    }
}
