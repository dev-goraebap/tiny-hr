package com.example.tinyhr.iam.adapter.web;

import com.example.tinyhr.iam.adapter.mapper.RoleQueryMapper;
import com.example.tinyhr.iam.adapter.mapper.viewmodel.RoleListItem;
import com.example.tinyhr.iam.application.RoleService;
import com.example.tinyhr.iam.application.dto.CreateRoleRequest;
import com.example.tinyhr.iam.application.dto.UpdateRoleRequest;
import com.example.tinyhr.shared.kernel.ApiResponse;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 역할(Role) 관리·조회 HTTP 진입점.
 *
 * <p>현재는 전 경로 permit — 권한 가드({@code ROLE_MANAGE} 요구)는 인증(3단계) 이식 후 연결한다.
 */
@RestController
@RequestMapping("/admin/roles")
public class RoleController {

    private final RoleService roleService;
    private final RoleQueryMapper roleQueryMapper;

    public RoleController(RoleService roleService, RoleQueryMapper roleQueryMapper) {
        this.roleService = roleService;
        this.roleQueryMapper = roleQueryMapper;
    }

    @GetMapping
    public ApiResponse<List<RoleListItem>> list() {
        return ApiResponse.of(roleQueryMapper.listForAdmin());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void create(@Valid @RequestBody CreateRoleRequest request) {
        roleService.create(request);
    }

    @PatchMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void update(@PathVariable String id, @Valid @RequestBody UpdateRoleRequest request) {
        roleService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@PathVariable String id) {
        roleService.archive(id);
    }

    @PostMapping("/{id}/archive")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void archive(@PathVariable String id) {
        roleService.archive(id);
    }

    @PostMapping("/{id}/reactivate")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reactivate(@PathVariable String id) {
        roleService.reactivate(id);
    }
}
