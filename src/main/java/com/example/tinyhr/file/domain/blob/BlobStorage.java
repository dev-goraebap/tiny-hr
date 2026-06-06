package com.example.tinyhr.file.domain.blob;

/**
 * 바이너리 본문 저장소 포트. 메타데이터({@link Blob})와 분리해 실제 바이트를 키 단위로 다룬다.
 *
 * <p>운영에서는 S3/파일시스템 어댑터로 구현한다. MVP 는 인메모리 어댑터를 쓴다.
 */
public interface BlobStorage {

    void store(String storageKey, byte[] content);

    byte[] load(String storageKey);

    void delete(String storageKey);
}
