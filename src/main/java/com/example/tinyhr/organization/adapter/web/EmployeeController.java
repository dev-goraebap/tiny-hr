package com.example.tinyhr.organization.adapter.web;

import com.example.tinyhr.organization.adapter.mapper.EmployeeQueryMapper;
import com.example.tinyhr.organization.adapter.mapper.viewmodel.EmployeeDetailView;
import com.example.tinyhr.organization.adapter.mapper.viewmodel.EmployeeListItem;
import com.example.tinyhr.organization.adapter.mapper.viewmodel.EmployeeListPage;
import com.example.tinyhr.organization.application.EmployeeService;
import com.example.tinyhr.organization.application.dto.CreateEmployeeRequest;
import com.example.tinyhr.organization.application.dto.UpdateEmployeeBasicRequest;
import com.example.tinyhr.organization.application.dto.UpdateEmployeeRequest;
import com.example.tinyhr.organization.domain.OrganizationErrorCode;
import com.example.tinyhr.organization.domain.employee.EmployeeStatus;
import com.example.tinyhr.shared.kernel.ApiResponse;
import com.example.tinyhr.shared.kernel.BusinessException;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** 사원 관리·조회 HTTP 진입점(단건 중심 MVP). */
@RestController
@RequestMapping("/admin/employees")
public class EmployeeController {

    private final EmployeeService employeeService;
    private final EmployeeQueryMapper employeeQueryMapper;

    public EmployeeController(
            EmployeeService employeeService, EmployeeQueryMapper employeeQueryMapper) {
        this.employeeService = employeeService;
        this.employeeQueryMapper = employeeQueryMapper;
    }

    @GetMapping
    public ApiResponse<EmployeeListPage> list(
            @RequestParam(required = false) EmployeeStatus status,
            @RequestParam(required = false) String departmentId,
            @RequestParam(required = false) String rankId,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "0") int offset) {
        int safeLimit = Math.min(Math.max(limit, 1), 1000);
        int safeOffset = Math.max(offset, 0);
        List<EmployeeListItem> items = employeeQueryMapper.listForAdmin(
                status, departmentId, rankId, q, sortBy, sortDir, safeLimit, safeOffset);
        long total = employeeQueryMapper.countForAdmin(status, departmentId, rankId, q);
        return ApiResponse.of(new EmployeeListPage(items, total));
    }

    @GetMapping("/{id}")
    public ApiResponse<EmployeeDetailView> getOne(@PathVariable String id) {
        EmployeeDetailView view = employeeQueryMapper.findDetail(id);
        if (view == null) {
            throw new BusinessException(OrganizationErrorCode.EMPLOYEE_NOT_FOUND);
        }
        return ApiResponse.of(view);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void create(@Valid @RequestBody CreateEmployeeRequest request) {
        employeeService.invite(request);
    }

    @PatchMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void update(@PathVariable String id, @Valid @RequestBody UpdateEmployeeRequest request) {
        employeeService.update(id, request);
    }

    @PatchMapping("/{id}/basic")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateBasic(
            @PathVariable String id, @Valid @RequestBody UpdateEmployeeBasicRequest request) {
        employeeService.updateBasic(id, request);
    }

    @PostMapping("/{id}/activate")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void activate(@PathVariable String id) {
        employeeService.activate(id);
    }

    @PostMapping("/{id}/terminate")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void terminate(@PathVariable String id) {
        employeeService.terminate(id);
    }

    @PostMapping("/{id}/clear-department")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void clearDepartment(@PathVariable String id) {
        employeeService.clearDepartment(id);
    }
}
