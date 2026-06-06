package com.example.tinyhr.file.domain.blob;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Blob 리포지토리. Spring Data JPA 가 구현을 생성한다. */
public interface BlobRepository extends JpaRepository<Blob, String> {

    /** 같은 체크섬의 blob(재사용 dedupe). */
    Optional<Blob> findByChecksum(String checksum);
}
