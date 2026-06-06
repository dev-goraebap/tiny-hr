package com.example.tinyhr.vacation.adapter.web;

import com.example.tinyhr.iam.adapter.security.AuthPrincipal;
import com.example.tinyhr.vacation.adapter.mapper.MeLeaveQueryMapper;
import com.example.tinyhr.vacation.adapter.mapper.viewmodel.MyLeaveItem;
import com.example.tinyhr.vacation.adapter.web.viewmodel.LeaveBalanceView;
import com.example.tinyhr.vacation.application.LeaveRequestService;
import com.example.tinyhr.vacation.application.dto.CreateLeaveRequest;
import com.example.tinyhr.shared.kernel.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 내 휴가 신청·조회 HTTP 진입점. {@code /me/**} 는 인증 필요(SecurityConfig).
 *
 * <p>휴가 승인/반려/취소/회수는 approval 의 {@code /approval/requests/{id}/...} 엔드포인트로 처리하며,
 * 그 확정이 {@code LeaveApprovalSpi} 를 통해 잔액·알림으로 이어진다.
 */
@RestController
@RequestMapping("/me")
public class MeLeaveController {

    private final LeaveRequestService leaveRequestService;
    private final MeLeaveQueryMapper meLeaveQueryMapper;

    public MeLeaveController(
            LeaveRequestService leaveRequestService, MeLeaveQueryMapper meLeaveQueryMapper) {
        this.leaveRequestService = leaveRequestService;
        this.meLeaveQueryMapper = meLeaveQueryMapper;
    }

    @PostMapping("/leaves")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<String> create(
            @AuthenticationPrincipal AuthPrincipal principal,
            @Valid @RequestBody CreateLeaveRequest request) {
        return ApiResponse.of(leaveRequestService.create(principal.userAccountId(), request));
    }

    @GetMapping("/leaves")
    public ApiResponse<List<MyLeaveItem>> myLeaves(
            @AuthenticationPrincipal AuthPrincipal principal) {
        return ApiResponse.of(meLeaveQueryMapper.listMyLeaves(principal.userAccountId()));
    }

    @GetMapping("/leave-balance")
    public ApiResponse<LeaveBalanceView> myBalance(
            @AuthenticationPrincipal AuthPrincipal principal) {
        Long units = meLeaveQueryMapper.findBalanceUnits(principal.userAccountId());
        return ApiResponse.of(LeaveBalanceView.ofUnits(units == null ? 0L : units));
    }
}
