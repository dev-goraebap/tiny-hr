package com.example.tinyhr.shared.kernel;

/** 권한 없음 (HTTP 403). */
public class ForbiddenException extends DomainException {
    public ForbiddenException(String message) {
        super(message);
    }
}
