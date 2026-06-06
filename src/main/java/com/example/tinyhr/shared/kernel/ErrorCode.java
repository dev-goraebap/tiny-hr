package com.example.tinyhr.shared.kernel;

import org.springframework.http.HttpStatus;

/**
 * 비즈니스 에러 카탈로그 단위. 컨텍스트별 enum 이 구현한다.
 * (예: {@code OrganizationErrorCode})
 */
public interface ErrorCode {

    /** 기계용 코드. enum 이면 보통 {@code name()}. */
    String code();

    HttpStatus status();

    String message();
}
