package com.example.tinyhr.iam.domain.useraccount;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 인증 계정 애그리거트 루트.
 *
 * <p>사원(Employee)과 1:1 — {@code id} 는 employeeId 와 같은 UUID 를 외부(organization 온보딩)에서
 * 받아 그대로 쓴다(도메인이 새로 발급하지 않는다). 도메인은 인증 채널 이메일과 활성 여부만 안다.
 */
@Entity
@Table(name = "user_account")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserAccount {

    @Id
    @Column(name = "user_account_id", length = 36)
    private String id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    /** 신규 사원 온보딩 시 계정 발급. 식별자는 호출자(=employeeId)가 제공한다. */
    public static UserAccount provision(String userAccountId, String email) {
        UserAccount account = new UserAccount();
        account.id = userAccountId;
        account.email = normalize(email);
        account.active = true;
        return account;
    }

    public void changeEmail(String newEmail) {
        this.email = normalize(newEmail);
    }

    public void activate() {
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
    }

    private static String normalize(String email) {
        return email.trim().toLowerCase();
    }
}
