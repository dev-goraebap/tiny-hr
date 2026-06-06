package com.example.tinyhr.iam.adapter.web;

import com.example.tinyhr.iam.adapter.security.AuthPrincipal;
import com.example.tinyhr.iam.application.RbacOpenHostService;
import com.example.tinyhr.iam.domain.rbac.Permission;
import com.example.tinyhr.shared.kernel.ApiResponse;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 현재 로그인 사용자의 유효 권한 조회. 프론트 메뉴·버튼 조건부 표시용.
 */
@RestController
@RequestMapping("/me")
public class MePermissionsController {

    private final RbacOpenHostService rbacOpenHostService;

    public MePermissionsController(RbacOpenHostService rbacOpenHostService) {
        this.rbacOpenHostService = rbacOpenHostService;
    }

    @GetMapping("/permissions")
    public ApiResponse<List<Permission>> myPermissions(
            @AuthenticationPrincipal AuthPrincipal principal) {
        return ApiResponse.of(rbacOpenHostService.listEffective(principal.userAccountId()));
    }
}
