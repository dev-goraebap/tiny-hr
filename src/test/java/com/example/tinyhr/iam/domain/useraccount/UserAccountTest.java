package com.example.tinyhr.iam.domain.useraccount;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UserAccountTest {

    @Test
    @DisplayName("계정 발급 시 이메일을 정규화하고 활성 상태로 만든다")
    void provision() {
        // when
        UserAccount account = UserAccount.provision("acc-1", "  User@Example.COM  ");

        // then
        assertThat(account.getId()).isEqualTo("acc-1");
        assertThat(account.getEmail()).isEqualTo("user@example.com");
        assertThat(account.isActive()).isTrue();
    }

    @Test
    @DisplayName("이메일을 변경하면 정규화된다")
    void changeEmail() {
        // given
        UserAccount account = UserAccount.provision("acc-1", "old@example.com");

        // when
        account.changeEmail("  New@Example.COM  ");

        // then
        assertThat(account.getEmail()).isEqualTo("new@example.com");
    }

    @Test
    @DisplayName("계정을 비활성화/재활성화할 수 있다")
    void activation() {
        // given
        UserAccount account = UserAccount.provision("acc-1", "user@example.com");

        // when
        account.deactivate();
        // then
        assertThat(account.isActive()).isFalse();

        // when
        account.activate();
        // then
        assertThat(account.isActive()).isTrue();
    }
}
