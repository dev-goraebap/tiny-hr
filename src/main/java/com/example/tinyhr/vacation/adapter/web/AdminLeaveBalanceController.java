package com.example.tinyhr.vacation.adapter.web;

import com.example.tinyhr.vacation.application.AnnualLeaveBalanceService;
import com.example.tinyhr.vacation.application.dto.GrantLeaveBalanceRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 관리자 연차 부여 HTTP 진입점. {@code /admin/**} 이라 ADMIN_PAGE_CONTROL 권한 필요(SecurityConfig).
 */
@RestController
@RequestMapping("/admin/leave-balances")
public class AdminLeaveBalanceController {

    private final AnnualLeaveBalanceService annualLeaveBalanceService;

    public AdminLeaveBalanceController(AnnualLeaveBalanceService annualLeaveBalanceService) {
        this.annualLeaveBalanceService = annualLeaveBalanceService;
    }

    @PostMapping("/{employeeId}/grant")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void grant(
            @PathVariable String employeeId, @Valid @RequestBody GrantLeaveBalanceRequest request) {
        annualLeaveBalanceService.grant(employeeId, request.days(), request.reason());
    }
}
