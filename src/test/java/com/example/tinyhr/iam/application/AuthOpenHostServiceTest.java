package com.example.tinyhr.iam.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.example.tinyhr.iam.domain.IamErrorCode;
import com.example.tinyhr.iam.domain.useraccount.UserAccount;
import com.example.tinyhr.iam.domain.useraccount.UserAccountRepository;
import com.example.tinyhr.shared.kernel.BusinessException;
import java.util.Optional;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuthOpenHostServiceTest {

    @Mock
    UserAccountRepository userAccountRepository;

    @InjectMocks
    AuthOpenHostService authOpenHostService;

    private static void assertBusiness(ThrowingCallable callable, IamErrorCode expected) {
        assertThatThrownBy(callable)
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(expected);
    }

    @Test
    @DisplayName("등록된 이메일이면 true 를 돌려준다(정규화 비교)")
    void isEmailRegistered() {
        given(userAccountRepository.findByEmail("user@example.com"))
                .willReturn(Optional.of(UserAccount.provision("acc-1", "user@example.com")));

        assertThat(authOpenHostService.isEmailRegistered("  User@Example.COM  ")).isTrue();
    }

    @Test
    @DisplayName("새 계정을 발급해 저장한다")
    void provisionAccount() {
        authOpenHostService.provisionAccount("acc-1", "user@example.com");
        then(userAccountRepository).should().save(any(UserAccount.class));
    }

    @Test
    @DisplayName("없는 계정의 이메일은 변경할 수 없다")
    void rejectChangeEmailWhenNotFound() {
        given(userAccountRepository.findById("none")).willReturn(Optional.empty());

        assertBusiness(() -> authOpenHostService.changeAccountEmail("none", "new@example.com"),
                IamErrorCode.USER_ACCOUNT_NOT_FOUND);
    }

    @Test
    @DisplayName("다른 계정이 쓰는 이메일로는 변경할 수 없다")
    void rejectChangeEmailWhenDuplicated() {
        UserAccount me = UserAccount.provision("acc-1", "me@example.com");
        UserAccount other = UserAccount.provision("acc-2", "taken@example.com");
        given(userAccountRepository.findById("acc-1")).willReturn(Optional.of(me));
        given(userAccountRepository.findByEmail("taken@example.com")).willReturn(Optional.of(other));

        assertBusiness(() -> authOpenHostService.changeAccountEmail("acc-1", "taken@example.com"),
                IamErrorCode.USER_ACCOUNT_EMAIL_DUPLICATED);
    }

    @Test
    @DisplayName("이메일을 변경하면 계정에 반영해 저장한다")
    void changeAccountEmail() {
        UserAccount me = UserAccount.provision("acc-1", "old@example.com");
        given(userAccountRepository.findById("acc-1")).willReturn(Optional.of(me));
        given(userAccountRepository.findByEmail("new@example.com")).willReturn(Optional.empty());

        authOpenHostService.changeAccountEmail("acc-1", "new@example.com");

        assertThat(me.getEmail()).isEqualTo("new@example.com");
        then(userAccountRepository).should().save(me);
    }

    @Test
    @DisplayName("같은 이메일로 변경 요청하면 아무것도 하지 않는다")
    void noopWhenSameEmail() {
        UserAccount me = UserAccount.provision("acc-1", "user@example.com");
        given(userAccountRepository.findById("acc-1")).willReturn(Optional.of(me));

        authOpenHostService.changeAccountEmail("acc-1", "User@Example.COM");

        then(userAccountRepository).should(never()).save(any());
    }
}
