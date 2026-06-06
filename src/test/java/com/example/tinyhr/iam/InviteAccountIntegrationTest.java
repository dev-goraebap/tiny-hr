package com.example.tinyhr.iam;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.example.tinyhr.iam.application.AuthOpenHostService;
import com.example.tinyhr.iam.application.AuthService;
import com.example.tinyhr.iam.domain.otpcode.OtpCodeRepository;
import com.example.tinyhr.iam.domain.useraccount.UserAccount;
import com.example.tinyhr.iam.domain.useraccount.UserAccountRepository;
import com.example.tinyhr.organization.application.EmployeeService;
import com.example.tinyhr.organization.application.dto.CreateEmployeeRequest;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * organization → iam 통합 검증.
 *
 * <p>사원 초대 시 같은 식별자로 인증 계정이 발급되어(userAccountId=employeeId), 이후 그 이메일로
 * OTP 발급(로그인 1단계)이 가능한지 확인한다.
 */
@SpringBootTest
@Transactional
class InviteAccountIntegrationTest {

    @Autowired EmployeeService employeeService;
    @Autowired AuthOpenHostService authOpenHostService;
    @Autowired AuthService authService;
    @Autowired UserAccountRepository userAccountRepository;
    @Autowired OtpCodeRepository otpCodeRepository;

    @Test
    @DisplayName("사원을 초대하면 인증 계정이 발급되고, 그 이메일로 OTP 를 발급할 수 있다")
    void inviteProvisionsAccountAndEnablesOtp() {
        String email = "invitee@example.com";
        String employeeId = employeeService.invite(
                new CreateEmployeeRequest("홍길동", email, null, null, LocalDate.of(2026, 1, 2)));

        // 1) 같은 식별자로 활성 계정이 발급되었다
        UserAccount account = userAccountRepository.findById(employeeId).orElseThrow();
        assertThat(account.getEmail()).isEqualTo(email);
        assertThat(account.isActive()).isTrue();
        assertThat(authOpenHostService.isEmailRegistered(email)).isTrue();

        // 2) 그 이메일로 OTP 발급이 가능하다(로그인 진입) — OTP 행이 생긴다
        assertThatCode(() -> authService.issueOtp(email)).doesNotThrowAnyException();
        assertThat(otpCodeRepository
                .findFirstByUserAccountIdAndConsumedAtIsNullAndRevokedAtIsNullOrderByIssuedAtDesc(
                        employeeId))
                .isPresent();
    }
}
