package com.example.tinyhr.shared.kernel;

/** 상태 충돌 (HTTP 409). */
public class ConflictException extends DomainException {
    public ConflictException(String message) {
        super(message);
    }
}
