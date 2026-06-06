package com.example.tinyhr.shared.config;

import com.example.tinyhr.shared.kernel.BadRequestException;
import com.example.tinyhr.shared.kernel.ConflictException;
import com.example.tinyhr.shared.kernel.DomainException;
import com.example.tinyhr.shared.kernel.ForbiddenException;
import com.example.tinyhr.shared.kernel.NotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** 도메인 예외를 상태별 HTTP 응답({@code {code, message}})으로 매핑한다. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    public record ErrorResponse(String code, String message) {}

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NotFoundException e) {
        return build(HttpStatus.NOT_FOUND, e);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflict(ConflictException e) {
        return build(HttpStatus.CONFLICT, e);
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ErrorResponse> handleForbidden(ForbiddenException e) {
        return build(HttpStatus.FORBIDDEN, e);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(BadRequestException e) {
        return build(HttpStatus.BAD_REQUEST, e);
    }

    /** 위 하위에 안 잡힌 도메인 예외 fallback. */
    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ErrorResponse> handleDomain(DomainException e) {
        return build(HttpStatus.BAD_REQUEST, e);
    }

    /** Bean Validation 실패. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .orElse("유효하지 않은 요청입니다");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("VALIDATION_FAILED", message));
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, DomainException e) {
        return ResponseEntity.status(status).body(new ErrorResponse(e.code(), e.getMessage()));
    }
}
