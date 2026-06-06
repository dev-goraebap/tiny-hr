package com.example.tinyhr.organization.adapter.web;

import com.example.tinyhr.organization.adapter.mapper.DepartmentQueryMapper;
import com.example.tinyhr.organization.adapter.mapper.viewmodel.DepartmentListItem;
import com.example.tinyhr.organization.application.DepartmentService;
import com.example.tinyhr.organization.application.dto.CreateDepartmentRequest;
import com.example.tinyhr.organization.application.dto.ReorderDepartmentsRequest;
import com.example.tinyhr.organization.application.dto.UpdateDepartmentRequest;
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

/** 부서(부서 &gt; 팀 2뎁스 트리) 관리·조회 HTTP 진입점. */
@RestController
@RequestMapping("/admin/departments")
public class DepartmentController {

    private final DepartmentService departmentService;
    private final DepartmentQueryMapper departmentQueryMapper;

    public DepartmentController(
            DepartmentService departmentService, DepartmentQueryMapper departmentQueryMapper) {
        this.departmentService = departmentService;
        this.departmentQueryMapper = departmentQueryMapper;
    }

    @GetMapping
    public ApiResponse<List<DepartmentListItem>> list() {
        return ApiResponse.of(departmentQueryMapper.listAll());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void create(@Valid @RequestBody CreateDepartmentRequest request) {
        departmentService.create(request);
    }

    @PostMapping("/reorder")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reorder(@Valid @RequestBody ReorderDepartmentsRequest request) {
        departmentService.reorderSiblings(request);
    }

    @PatchMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void update(@PathVariable String id, @Valid @RequestBody UpdateDepartmentRequest request) {
        departmentService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@PathVariable String id) {
        departmentService.archive(id);
    }

    @PostMapping("/{id}/archive")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void archive(@PathVariable String id) {
        departmentService.archive(id);
    }
}
