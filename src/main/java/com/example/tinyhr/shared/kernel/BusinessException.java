package com.example.tinyhr.shared.kernel;

import lombok.Getter;

/**
 * 도메인/유즈케이스 비즈니스 규칙 위반. 구체 에러는 {@link ErrorCode} 로 식별한다.
 * 에러마다 별도 예외 클래스를 만들지 않는다.
 */
@Getter
public class BusinessException extends RuntimeException {

    private final transient ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.message());
        this.errorCode = errorCode;
    }
}
