package com.example.tinyhr.file.domain;

import com.example.tinyhr.shared.kernel.ErrorCode;
import org.springframework.http.HttpStatus;

/** file 컨텍스트 에러 카탈로그. */
public enum FileErrorCode implements ErrorCode {

    FILE_REQUIRED(HttpStatus.BAD_REQUEST, "파일이 필요합니다"),
    EMPTY_FILE(HttpStatus.BAD_REQUEST, "빈 파일은 업로드할 수 없습니다"),
    FILE_TOO_LARGE(HttpStatus.BAD_REQUEST, "파일 크기가 최대치를 초과합니다"),
    BLOB_INVALID(HttpStatus.BAD_REQUEST, "파일 메타데이터가 올바르지 않습니다"),
    ATTACHMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "첨부를 찾을 수 없습니다"),
    BLOB_NOT_FOUND(HttpStatus.NOT_FOUND, "파일 본문을 찾을 수 없습니다"),
    ATTACHMENT_UPLOAD_FORBIDDEN(HttpStatus.FORBIDDEN, "첨부 업로드 권한이 없습니다"),
    ATTACHMENT_READ_FORBIDDEN(HttpStatus.FORBIDDEN, "첨부 조회 권한이 없습니다"),
    ATTACHMENT_DELETE_FORBIDDEN(HttpStatus.FORBIDDEN, "첨부 삭제 권한이 없습니다"),
    ATTACHMENT_TRANSFER_FORBIDDEN(HttpStatus.FORBIDDEN, "첨부를 이관할 수 없습니다");

    private final HttpStatus status;
    private final String message;

    FileErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    @Override
    public String code() {
        return name();
    }

    @Override
    public HttpStatus status() {
        return status;
    }

    @Override
    public String message() {
        return message;
    }
}
