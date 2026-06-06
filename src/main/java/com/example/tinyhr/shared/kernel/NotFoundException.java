package com.example.tinyhr.shared.kernel;

/** 리소스를 찾을 수 없음 (HTTP 404). */
public class NotFoundException extends DomainException {
    public NotFoundException(String message) {
        super(message);
    }
}
