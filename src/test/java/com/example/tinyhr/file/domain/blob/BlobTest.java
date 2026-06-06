package com.example.tinyhr.file.domain.blob;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.tinyhr.file.domain.FileErrorCode;
import com.example.tinyhr.shared.kernel.BusinessException;
import java.time.Instant;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class BlobTest {

    private static final String CHECKSUM =
            "a".repeat(64); // 64 hex

    private static void assertInvalid(ThrowingCallable callable) {
        assertThatThrownBy(callable)
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(FileErrorCode.BLOB_INVALID);
    }

    @Test
    @DisplayName("유효한 메타로 blob 을 생성한다")
    void create() {
        Blob b = Blob.create("key/1", "  photo.png ", "image/png", 1024, CHECKSUM.toUpperCase());

        assertThat(b.getId()).isNotBlank();
        assertThat(b.getFilename()).isEqualTo("photo.png");
        assertThat(b.getChecksum()).isEqualTo(CHECKSUM); // 소문자화
        assertThat(b.isDeleted()).isFalse();
    }

    @Test
    @DisplayName("파일명이 비면 생성할 수 없다")
    void create_blankFilename() {
        assertInvalid(() -> Blob.create("key", "  ", "image/png", 10, CHECKSUM));
    }

    @Test
    @DisplayName("크기가 0 이하면 생성할 수 없다")
    void create_invalidSize() {
        assertInvalid(() -> Blob.create("key", "a.png", "image/png", 0, CHECKSUM));
    }

    @Test
    @DisplayName("체크섬이 sha256 hex 가 아니면 생성할 수 없다")
    void create_invalidChecksum() {
        assertInvalid(() -> Blob.create("key", "a.png", "image/png", 10, "not-a-checksum"));
    }

    @Test
    @DisplayName("소프트 삭제는 멱등이다")
    void softDelete() {
        Blob b = Blob.create("key", "a.png", "image/png", 10, CHECKSUM);
        Instant t0 = Instant.parse("2026-01-01T00:00:00Z");

        b.softDelete(t0);
        b.softDelete(t0.plusSeconds(10));

        assertThat(b.isDeleted()).isTrue();
        assertThat(b.getRevokedAt()).isEqualTo(t0);
    }
}
