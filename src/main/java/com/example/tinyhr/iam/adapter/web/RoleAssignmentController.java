package com.example.tinyhr.iam.adapter.web;

import com.example.tinyhr.iam.adapter.mapper.RoleAssignmentQueryMapper;
import com.example.tinyhr.iam.adapter.mapper.viewmodel.RoleAssignmentListItem;
import com.example.tinyhr.iam.adapter.mapper.viewmodel.RoleAssignmentMember;
import com.example.tinyhr.iam.application.RoleAssignmentService;
import com.example.tinyhr.iam.application.dto.AssignRoleRequest;
import com.example.tinyhr.shared.kernel.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 역할 부여(RoleAssignment) 관리·조회 HTTP 진입점.
 *
 * <p>현재는 전 경로 permit — 권한 가드({@code ROLE_MANAGE} 요구)는 인증(3단계) 이식 후 연결한다.
 */
@RestController
@RequestMapping("/admin/role-assignments")
public class RoleAssignmentController {

    private final RoleAssignmentService roleAssignmentService;
    private final RoleAssignmentQueryMapper roleAssignmentQueryMapper;

    public RoleAssignmentController(
            RoleAssignmentService roleAssignmentService,
            RoleAssignmentQueryMapper roleAssignmentQueryMapper) {
        this.roleAssignmentService = roleAssignmentService;
        this.roleAssignmentQueryMapper = roleAssignmentQueryMapper;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void assign(@Valid @RequestBody AssignRoleRequest request) {
        roleAssignmentService.assign(request);
    }

    @DeleteMapping("/{assignmentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revoke(@PathVariable String assignmentId) {
        roleAssignmentService.revoke(assignmentId);
    }

    @GetMapping("/users/{userId}")
    public ApiResponse<List<RoleAssignmentListItem>> listByUser(@PathVariable String userId) {
        return ApiResponse.of(roleAssignmentQueryMapper.listByUser(userId));
    }

    @GetMapping("/roles/{roleId}")
    public ApiResponse<List<RoleAssignmentMember>> listByRole(@PathVariable String roleId) {
        return ApiResponse.of(roleAssignmentQueryMapper.listMembersByRole(roleId));
    }
}
