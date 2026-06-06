package com.example.tinyhr.iam.application;

import com.example.tinyhr.iam.application.dto.SessionPairResult;
import com.example.tinyhr.iam.application.dto.SessionWithUserResult;
import com.example.tinyhr.iam.config.AuthProperties;
import com.example.tinyhr.iam.domain.IamErrorCode;
import com.example.tinyhr.iam.domain.TokenHasher;
import com.example.tinyhr.iam.domain.auth.AccessTokenService;
import com.example.tinyhr.iam.domain.auth.EmployeeStatusReader;
import com.example.tinyhr.iam.domain.auth.EmployeeStatusReader.LoginStatus;
import com.example.tinyhr.iam.domain.auth.OtpSender;
import com.example.tinyhr.iam.domain.otpcode.OtpCode;
import com.example.tinyhr.iam.domain.otpcode.OtpCodeRepository;
import com.example.tinyhr.iam.domain.refreshtoken.RefreshToken;
import com.example.tinyhr.iam.domain.refreshtoken.RefreshTokenRepository;
import com.example.tinyhr.iam.domain.useraccount.UserAccount;
import com.example.tinyhr.iam.domain.useraccount.UserAccountRepository;
import com.example.tinyhr.shared.kernel.BusinessException;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 인증 워크플로우 오케스트레이션 — OTP 발급/검증, refresh 회전, 세션 폐기, 개발 세션.
 *
 * <p>비밀번호 없는 OTP 로그인: {@code issueOtp} → {@code verifyOtp}(세션 발급) → {@code rotate}
 * (슬라이딩 만료) → {@code logout}/{@code logoutAll}. raw OTP·refresh 는 1회만 노출하고 DB 에는
 * 해시만 저장한다.
 *
 * @actor 일반사원, 관리자
 */
@Service
@Transactional
public class AuthService {

    private final UserAccountRepository userAccountRepository;
    private final OtpCodeRepository otpCodeRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AccessTokenService accessTokenService;
    private final EmployeeStatusReader employeeStatusReader;
    private final OtpSender otpSender;
    private final AuthProperties props;

    public AuthService(
            UserAccountRepository userAccountRepository,
            OtpCodeRepository otpCodeRepository,
            RefreshTokenRepository refreshTokenRepository,
            AccessTokenService accessTokenService,
            EmployeeStatusReader employeeStatusReader,
            OtpSender otpSender,
            AuthProperties props) {
        this.userAccountRepository = userAccountRepository;
        this.otpCodeRepository = otpCodeRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.accessTokenService = accessTokenService;
        this.employeeStatusReader = employeeStatusReader;
        this.otpSender = otpSender;
        this.props = props;
    }

    /** 이메일에 대해 6자리 OTP 를 발급해 발송한다. 사원 상태 게이트는 verify 시점에 둔다. */
    public void issueOtp(String email) {
        String normalized = normalize(email);
        UserAccount account = userAccountRepository.findByEmail(normalized)
                .orElseThrow(() -> new BusinessException(IamErrorCode.AUTH_USER_ACCOUNT_NOT_FOUND));
        if (!account.isActive()) {
            throw new BusinessException(IamErrorCode.AUTH_USER_ACCOUNT_NOT_ACTIVE);
        }

        Instant now = Instant.now();

        long cooldown = props.otpResendCooldownSeconds();
        if (cooldown > 0) {
            otpCodeRepository
                    .findFirstByUserAccountIdAndConsumedAtIsNullAndRevokedAtIsNullOrderByIssuedAtDesc(
                            account.getId())
                    .filter(existing -> existing.isActive(now))
                    .ifPresent(existing -> {
                        long since = Duration.between(existing.getIssuedAt(), now).getSeconds();
                        if (since < cooldown) {
                            throw new BusinessException(IamErrorCode.OTP_RESEND_COOLDOWN);
                        }
                    });
        }

        otpCodeRepository.revokeAllActiveForUser(account.getId(), now);

        String rawCode = TokenHasher.generateOtpCode();
        OtpCode otp = OtpCode.issue(
                account.getId(), TokenHasher.hashToken(rawCode), now, props.otpTtlSeconds());
        otpCodeRepository.save(otp);

        otpSender.sendOtp(account.getEmail(), rawCode, props.otpTtlSeconds());
    }

    /** OTP 검증 후 사원 ACTIVE 게이트를 통과하면 세션을 발급한다. */
    public SessionWithUserResult verifyOtp(String email, String code) {
        String normalized = normalize(email);
        // 미등록 이메일도 OTP_INVALID 로 통일 — 검증 단계에서 enumeration 차단.
        UserAccount account = userAccountRepository.findByEmail(normalized)
                .orElseThrow(() -> new BusinessException(IamErrorCode.OTP_INVALID));
        if (!account.isActive()) {
            throw new BusinessException(IamErrorCode.AUTH_USER_ACCOUNT_NOT_ACTIVE);
        }

        OtpCode otp = otpCodeRepository
                .findFirstByUserAccountIdAndConsumedAtIsNullAndRevokedAtIsNullOrderByIssuedAtDesc(
                        account.getId())
                .orElseThrow(() -> new BusinessException(IamErrorCode.OTP_INVALID));

        Instant now = Instant.now();
        otp.verify(TokenHasher.hashToken(code), props.otpMaxAttempts(), now);
        otpCodeRepository.save(otp);

        gateEmployeeForLogin(account.getId());

        return issueSession(account.getId(), now);
    }

    /** refresh token 회전(슬라이딩 만료). 이미 회전된 토큰 재제출은 경합으로 보고 세션을 유지한다. */
    public SessionPairResult rotateRefreshToken(String rawRefreshToken) {
        String tokenHash = TokenHasher.hashToken(rawRefreshToken);
        RefreshToken existing = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new BusinessException(IamErrorCode.REFRESH_TOKEN_INVALID));

        Instant now = Instant.now();
        try {
            String newTokenId = UUID.randomUUID().toString();
            existing.rotate(newTokenId, now);
            refreshTokenRepository.save(existing);
            return issueSessionPair(existing, now, newTokenId);
        } catch (BusinessException e) {
            // 이미 회전된 토큰 재제출(경합/재시작): 세션을 끊지 않고 새 토큰을 재발급한다.
            // 폐기·만료는 rotate 가 REFRESH_TOKEN_INVALID 로 먼저 거른다.
            if (e.getErrorCode() == IamErrorCode.REFRESH_TOKEN_REUSED) {
                if (!now.isBefore(existing.getExpiresAt())) {
                    throw new BusinessException(IamErrorCode.REFRESH_TOKEN_INVALID);
                }
                return issueSessionPair(existing, now, UUID.randomUUID().toString());
            }
            throw e;
        }
    }

    /** 본인의 현재 세션 하나를 무효화한다(로그아웃). */
    public void revokeSession(String sessionId) {
        refreshTokenRepository.revokeSession(sessionId, Instant.now());
    }

    /** 사원의 모든 세션을 무효화한다(전체 로그아웃·강제 로그아웃·퇴사). */
    public void revokeAllSessions(String userAccountId) {
        refreshTokenRepository.revokeAllForUser(userAccountId, Instant.now());
    }

    /** 개발 전용 — 인증 없이 ACTIVE 사원의 세션을 즉시 발급한다. */
    public SessionWithUserResult issueDevSession(String userAccountId) {
        LoginStatus status = employeeStatusReader.findStatus(userAccountId)
                .orElseThrow(() -> new BusinessException(IamErrorCode.AUTH_USER_ACCOUNT_NOT_FOUND));
        if (status != LoginStatus.ACTIVE) {
            throw new BusinessException(IamErrorCode.AUTH_USER_ACCOUNT_NOT_ACTIVE);
        }
        return issueSession(userAccountId, Instant.now());
    }

    private void gateEmployeeForLogin(String userAccountId) {
        LoginStatus status = employeeStatusReader.findStatus(userAccountId)
                .orElseThrow(() -> new BusinessException(IamErrorCode.AUTH_USER_ACCOUNT_NOT_FOUND));
        switch (status) {
            case INVITED -> throw new BusinessException(IamErrorCode.AUTH_EMPLOYEE_NOT_YET_ACTIVE);
            case TERMINATED -> throw new BusinessException(IamErrorCode.AUTH_EMPLOYEE_TERMINATED);
            case ACTIVE -> { /* 통과 */ }
        }
    }

    /** 신규 세션(새 sessionId) 발급. */
    private SessionWithUserResult issueSession(String userAccountId, Instant now) {
        String sessionId = UUID.randomUUID().toString();
        String rawRefresh = TokenHasher.generateRawToken();
        RefreshToken refresh = RefreshToken.issue(
                UUID.randomUUID().toString(),
                sessionId,
                userAccountId,
                TokenHasher.hashToken(rawRefresh),
                now,
                props.jwtRefreshTtlSeconds());
        refreshTokenRepository.save(refresh);

        String accessToken = accessTokenService.sign(userAccountId, sessionId);
        return new SessionWithUserResult(
                userAccountId, accessToken, rawRefresh, refresh.getExpiresAt());
    }

    /** 같은 세션에 새 refresh + access 발급(회전 결과). */
    private SessionPairResult issueSessionPair(RefreshToken existing, Instant now, String newTokenId) {
        String rawNew = TokenHasher.generateRawToken();
        RefreshToken newToken = RefreshToken.issue(
                newTokenId,
                existing.getSessionId(),
                existing.getUserAccountId(),
                TokenHasher.hashToken(rawNew),
                now,
                props.jwtRefreshTtlSeconds());
        refreshTokenRepository.save(newToken);

        String accessToken =
                accessTokenService.sign(existing.getUserAccountId(), existing.getSessionId());
        return new SessionPairResult(accessToken, rawNew, newToken.getExpiresAt());
    }

    private static String normalize(String email) {
        return email.trim().toLowerCase();
    }
}
