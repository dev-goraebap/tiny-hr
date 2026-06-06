package com.example.tinyhr.file.adapter.persistence;

import com.example.tinyhr.file.domain.FileErrorCode;
import com.example.tinyhr.file.domain.blob.BlobStorage;
import com.example.tinyhr.shared.kernel.BusinessException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * {@link BlobStorage} 의 MVP 인메모리 구현. 프로세스 메모리에 키-바이트로 보관한다(재시작 시 소실).
 *
 * <p>운영에서는 S3/파일시스템 어댑터로 교체한다.
 */
@Component
public class InMemoryBlobStorage implements BlobStorage {

    private final Map<String, byte[]> store = new ConcurrentHashMap<>();

    @Override
    public void store(String storageKey, byte[] content) {
        store.put(storageKey, content.clone());
    }

    @Override
    public byte[] load(String storageKey) {
        byte[] content = store.get(storageKey);
        if (content == null) {
            throw new BusinessException(FileErrorCode.BLOB_NOT_FOUND);
        }
        return content.clone();
    }

    @Override
    public void delete(String storageKey) {
        store.remove(storageKey);
    }
}
