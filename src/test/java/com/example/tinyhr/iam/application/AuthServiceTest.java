package com.example.tinyhr.iam.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

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
import java.time.Instant;
import java.util.Optional;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserAccountRepository userAccountRepository;
    @Mock OtpCodeRepository otpCodeRepository;
    @Mock RefreshTokenRepository refreshTokenRepository;
    @Mock AccessTokenService accessTokenService;
    @Mock EmployeeStatusReader employeeStatusReader;
    @Mock OtpSender otpSender;

    AuthService authService;

    private final AuthProperties props =
            new AuthProperties(300, 5, 60, "dev-secret", 900, 3600, true);

    @BeforeEach
    void setUp() {
        authService = new AuthService(userAccountRepository, otpCodeRepository,
                refreshTokenRepository, accessTokenService, employeeStatusReader, otpSender, props);
    }

    private static void assertBusiness(ThrowingCallable callable, IamErrorCode expected) {
        assertThatThrownBy(callable)
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(expected);
    }

    private static UserAccount activeAccount() {
        return UserAccount.provision("user-1", "user@example.com");
    }

    // ---- issueOtp ----

    @Test
    @DisplayName("미등록 이메일이면 발급할 수 없다")
    void issueOtp_notFound() {
        given(userAccountRepository.findByEmail("user@example.com")).willReturn(Optional.empty());

        assertBusiness(() -> authService.issueOtp("User@Example.com"),
                IamErrorCode.AUTH_USER_ACCOUNT_NOT_FOUND);
    }

    @Test
    @DisplayName("비활성 계정이면 발급할 수 없다")
    void issueOtp_notActive() {
        UserAccount account = activeAccount();
        account.deactivate();
        given(userAccountRepository.findByEmail("user@example.com")).willReturn(Optional.of(account));

        assertBusiness(() -> authService.issueOtp("user@example.com"),
                IamErrorCode.AUTH_USER_ACCOUNT_NOT_ACTIVE);
    }

    @Test
    @DisplayName("쿨다운 이내 재요청은 거부된다")
    void issueOtp_cooldown() {
        given(userAccountRepository.findByEmail("user@example.com"))
                .willReturn(Optional.of(activeAccount()));
        OtpCode recent = OtpCode.issue("user-1", "hash", Instant.now(), 300);
        given(otpCodeRepository
                .findFirstByUserAccountIdAndConsumedAtIsNullAndRevokedAtIsNullOrderByIssuedAtDesc(
                        "user-1"))
                .willReturn(Optional.of(recent));

        assertBusiness(() -> authService.issueOtp("user@example.com"),
                IamErrorCode.OTP_RESEND_COOLDOWN);
    }

    @Test
    @DisplayName("OTP 를 발급·저장하고 발송한다")
    void issueOtp_success() {
        given(userAccountRepository.findByEmail("user@example.com"))
                .willReturn(Optional.of(activeAccount()));
        given(otpCodeRepository
                .findFirstByUserAccountIdAndConsumedAtIsNullAndRevokedAtIsNullOrderByIssuedAtDesc(
                        "user-1"))
                .willReturn(Optional.empty());

        authService.issueOtp("user@example.com");

        then(otpCodeRepository).should().save(any(OtpCode.class));
        then(otpSender).should().sendOtp(eq("user@example.com"), anyString(), eq(300L));
    }

    // ---- verifyOtp ----

    @Test
    @DisplayName("미등록 이메일 검증은 OTP_INVALID 로 통일한다")
    void verifyOtp_accountNotFound() {
        given(userAccountRepository.findByEmail("user@example.com")).willReturn(Optional.empty());

        assertBusiness(() -> authService.verifyOtp("user@example.com", "123456"),
                IamErrorCode.OTP_INVALID);
    }

    @Test
    @DisplayName("활성 OTP 가 없으면 OTP_INVALID")
    void verifyOtp_noOtp() {
        given(userAccountRepository.findByEmail("user@example.com"))
                .willReturn(Optional.of(activeAccount()));
        given(otpCodeRepository
                .findFirstByUserAccountIdAndConsumedAtIsNullAndRevokedAtIsNullOrderByIssuedAtDesc(
                        "user-1"))
                .willReturn(Optional.empty());

        assertBusiness(() -> authService.verifyOtp("user@example.com", "123456"),
                IamErrorCode.OTP_INVALID);
    }

    @Test
    @DisplayName("초대 상태 사원은 로그인 게이트에서 막힌다")
    void verifyOtp_employeeInvited() {
        given(userAccountRepository.findByEmail("user@example.com"))
                .willReturn(Optional.of(activeAccount()));
        OtpCode otp = OtpCode.issue("user-1", TokenHasher.hashToken("123456"), Instant.now(), 300);
        given(otpCodeRepository
                .findFirstByUserAccountIdAndConsumedAtIsNullAndRevokedAtIsNullOrderByIssuedAtDesc(
                        "user-1"))
                .willReturn(Optional.of(otp));
        given(employeeStatusReader.findStatus("user-1")).willReturn(Optional.of(LoginStatus.INVITED));

        assertBusiness(() -> authService.verifyOtp("user@example.com", "123456"),
                IamErrorCode.AUTH_EMPLOYEE_NOT_YET_ACTIVE);
    }

    @Test
    @DisplayName("코드가 맞고 사원이 ACTIVE 면 세션을 발급한다")
    void verifyOtp_success() {
        given(userAccountRepository.findByEmail("user@example.com"))
                .willReturn(Optional.of(activeAccount()));
        OtpCode otp = OtpCode.issue("user-1", TokenHasher.hashToken("123456"), Instant.now(), 300);
        given(otpCodeRepository
                .findFirstByUserAccountIdAndConsumedAtIsNullAndRevokedAtIsNullOrderByIssuedAtDesc(
                        "user-1"))
                .willReturn(Optional.of(otp));
        given(employeeStatusReader.findStatus("user-1")).willReturn(Optional.of(LoginStatus.ACTIVE));
        given(accessTokenService.sign(eq("user-1"), anyString())).willReturn("access-token");

        SessionWithUserResult result = authService.verifyOtp("user@example.com", "123456");

        assertThat(result.userAccountId()).isEqualTo("user-1");
        assertThat(result.accessToken()).isEqualTo("access-token");
        assertThat(result.refreshToken()).isNotBlank();
        assertThat(otp.getConsumedAt()).isNotNull();
        then(refreshTokenRepository).should().save(any(RefreshToken.class));
    }

    // ---- rotateRefreshToken ----

    @Test
    @DisplayName("없는 리프레시 토큰은 INVALID")
    void rotate_invalid() {
        given(refreshTokenRepository.findByTokenHash(anyString())).willReturn(Optional.empty());

        assertBusiness(() -> authService.rotateRefreshToken("raw"),
                IamErrorCode.REFRESH_TOKEN_INVALID);
    }

    @Test
    @DisplayName("활성 토큰은 회전되어 새 페어를 발급한다")
    void rotate_success() {
        RefreshToken existing =
                RefreshToken.issue("tok-1", "sess-1", "user-1", "hash", Instant.now(), 3600);
        given(refreshTokenRepository.findByTokenHash(anyString())).willReturn(Optional.of(existing));
        given(accessTokenService.sign("user-1", "sess-1")).willReturn("access-token");

        SessionPairResult result = authService.rotateRefreshToken("raw");

        assertThat(result.accessToken()).isEqualTo("access-token");
        assertThat(result.refreshToken()).isNotBlank();
        assertThat(existing.getRotatedToId()).isNotNull();
    }

    @Test
    @DisplayName("이미 회전된 토큰 재제출은 세션을 끊지 않고 재발급한다")
    void rotate_reuseReissues() {
        RefreshToken existing =
                RefreshToken.issue("tok-1", "sess-1", "user-1", "hash", Instant.now(), 3600);
        existing.rotate("tok-2", Instant.now());
        given(refreshTokenRepository.findByTokenHash(anyString())).willReturn(Optional.of(existing));
        given(accessTokenService.sign("user-1", "sess-1")).willReturn("access-token");

        SessionPairResult result = authService.rotateRefreshToken("raw");

        assertThat(result.refreshToken()).isNotBlank();
    }

    // ---- dev session ----

    @Test
    @DisplayName("개발 세션은 없는 사원에 발급할 수 없다")
    void dev_notFound() {
        given(employeeStatusReader.findStatus("user-1")).willReturn(Optional.empty());

        assertBusiness(() -> authService.issueDevSession("user-1"),
                IamErrorCode.AUTH_USER_ACCOUNT_NOT_FOUND);
    }

    @Test
    @DisplayName("개발 세션은 ACTIVE 가 아니면 발급할 수 없다")
    void dev_notActive() {
        given(employeeStatusReader.findStatus("user-1"))
                .willReturn(Optional.of(LoginStatus.TERMINATED));

        assertBusiness(() -> authService.issueDevSession("user-1"),
                IamErrorCode.AUTH_USER_ACCOUNT_NOT_ACTIVE);
    }

    @Test
    @DisplayName("개발 세션을 발급한다")
    void dev_success() {
        given(employeeStatusReader.findStatus("user-1")).willReturn(Optional.of(LoginStatus.ACTIVE));
        given(accessTokenService.sign(eq("user-1"), anyString())).willReturn("access-token");

        SessionWithUserResult result = authService.issueDevSession("user-1");

        assertThat(result.userAccountId()).isEqualTo("user-1");
        assertThat(result.accessToken()).isEqualTo("access-token");
        then(refreshTokenRepository).should().save(any(RefreshToken.class));
    }

    // ---- logout ----

    @Test
    @DisplayName("로그아웃은 세션 폐기를 위임한다")
    void revokeSession() {
        authService.revokeSession("sess-1");
        then(refreshTokenRepository).should().revokeSession(eq("sess-1"), any(Instant.class));
    }

    @Test
    @DisplayName("전체 로그아웃은 사용자 전체 폐기를 위임한다")
    void revokeAll() {
        authService.revokeAllSessions("user-1");
        then(refreshTokenRepository).should().revokeAllForUser(eq("user-1"), any(Instant.class));
    }
}
