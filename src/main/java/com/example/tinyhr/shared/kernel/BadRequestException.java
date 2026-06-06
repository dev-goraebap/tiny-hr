package com.example.tinyhr.shared.kernel;

/** 잘못된 요청·입력 (HTTP 400). */
public class BadRequestException extends DomainException {
    public BadRequestException(String message) {
        super(message);
    }
}
