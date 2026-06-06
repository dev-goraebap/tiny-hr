package com.example.tinyhr.iam.adapter.web;

import com.example.tinyhr.iam.adapter.security.AuthPrincipal;
import com.example.tinyhr.iam.application.AuthService;
import com.example.tinyhr.iam.application.dto.DevSessionRequest;
import com.example.tinyhr.iam.application.dto.IssueOtpRequest;
import com.example.tinyhr.iam.application.dto.RefreshTokenRequest;
import com.example.tinyhr.iam.application.dto.SessionPairResult;
import com.example.tinyhr.iam.application.dto.SessionWithUserResult;
import com.example.tinyhr.iam.application.dto.VerifyOtpRequest;
import com.example.tinyhr.iam.config.AuthProperties;
import com.example.tinyhr.shared.kernel.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * 인증 HTTP 진입점 — OTP 로그인, 토큰 회전, 로그아웃, 개발 세션.
 *
 * <p>공개 엔드포인트(otp/refresh/dev-session)는 {@code SecurityConfig} 에서 permitAll,
 * logout 계열은 인증 필요.
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final AuthProperties props;

    public AuthController(AuthService authService, AuthProperties props) {
        this.authService = authService;
        this.props = props;
    }

    @PostMapping("/otp/issue")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void issueOtp(@Valid @RequestBody IssueOtpRequest request) {
        authService.issueOtp(request.email());
    }

    @PostMapping("/otp/verify")
    public ApiResponse<SessionWithUserResult> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        return ApiResponse.of(authService.verifyOtp(request.email(), request.code()));
    }

    @PostMapping("/refresh")
    public ApiResponse<SessionPairResult> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ApiResponse.of(authService.rotateRefreshToken(request.rawRefreshToken()));
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@AuthenticationPrincipal AuthPrincipal principal) {
        authService.revokeSession(principal.sessionId());
    }

    @PostMapping("/logout-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logoutAll(@AuthenticationPrincipal AuthPrincipal principal) {
        authService.revokeAllSessions(principal.userAccountId());
    }

    @PostMapping("/dev-session")
    public ApiResponse<SessionWithUserResult> devSession(
            @Valid @RequestBody DevSessionRequest request) {
        if (!props.devLoginEnabled()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return ApiResponse.of(authService.issueDevSession(request.userAccountId()));
    }
}
