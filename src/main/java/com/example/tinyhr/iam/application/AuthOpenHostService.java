package com.example.tinyhr.iam.application;

import com.example.tinyhr.iam.domain.IamErrorCode;
import com.example.tinyhr.iam.domain.useraccount.UserAccount;
import com.example.tinyhr.iam.domain.useraccount.UserAccountRepository;
import com.example.tinyhr.shared.kernel.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * iam 컨텍스트가 외부에 공개하는 Open Host Service.
 *
 * <p>인증 계정(UserAccount) 라이프사이클을 다른 컨텍스트(organization 온보딩 등)가 안전하게
 * 다루도록 노출한다. 계정 불변식(중복/존재)은 iam 이 소유하며, 외부는 UserAccountRepository 를
 * 직접 만지지 않는다.
 */
@Service
@Transactional
public class AuthOpenHostService {

    private final UserAccountRepository userAccountRepository;

    public AuthOpenHostService(UserAccountRepository userAccountRepository) {
        this.userAccountRepository = userAccountRepository;
    }

    /** 인증 계정 이메일이 이미 등록돼 있는지(가입 전 사전 검사용). */
    @Transactional(readOnly = true)
    public boolean isEmailRegistered(String email) {
        return userAccountRepository.findByEmail(normalize(email)).isPresent();
    }

    /** 신규 사원 온보딩 시 인증 계정을 발급한다(userAccountId == employeeId). */
    public void provisionAccount(String userAccountId, String email) {
        UserAccount account = UserAccount.provision(userAccountId, email);
        userAccountRepository.save(account);
    }

    /** 사원 이메일 변경에 맞춰 인증 계정 이메일을 동기화한다(중복 충돌 방어). */
    public void changeAccountEmail(String userAccountId, String email) {
        String normalized = normalize(email);
        UserAccount account = userAccountRepository.findById(userAccountId)
                .orElseThrow(() -> new BusinessException(IamErrorCode.USER_ACCOUNT_NOT_FOUND));
        if (account.getEmail().equals(normalized)) {
            return;
        }
        userAccountRepository.findByEmail(normalized).ifPresent(colliding -> {
            if (!colliding.getId().equals(userAccountId)) {
                throw new BusinessException(IamErrorCode.USER_ACCOUNT_EMAIL_DUPLICATED);
            }
        });
        account.changeEmail(normalized);
        userAccountRepository.save(account);
    }

    private static String normalize(String email) {
        return email.trim().toLowerCase();
    }
}
