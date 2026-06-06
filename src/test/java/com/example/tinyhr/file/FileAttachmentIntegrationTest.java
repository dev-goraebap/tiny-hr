package com.example.tinyhr.file;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.tinyhr.file.application.FileService;
import com.example.tinyhr.file.application.FileService.Download;
import com.example.tinyhr.file.application.FileService.UploadResult;
import com.example.tinyhr.file.domain.AttachmentOwnerType;
import com.example.tinyhr.file.domain.FileErrorCode;
import com.example.tinyhr.shared.kernel.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * file ← organization SPI(DIP) 통합 검증.
 *
 * <p>EMPLOYEE_PROFILE 첨부의 접근 인가는 organization 의 {@code EmployeeProfileAttachmentAccessSpi}
 * (자동 등록)가 "본인만" 으로 판정한다. 본인은 업로드·다운로드·삭제할 수 있고, 타인은 거부된다.
 */
@SpringBootTest
@Transactional
class FileAttachmentIntegrationTest {

    @Autowired FileService fileService;

    private static final byte[] CONTENT = "profile-photo-bytes".getBytes();

    @Test
    @DisplayName("본인은 프로필 첨부를 업로드·다운로드·삭제할 수 있고, 타인은 거부된다")
    void ownerCanManageProfileAttachment() {
        String emp = "emp-file-1";

        // 본인 업로드
        UploadResult uploaded = fileService.upload(
                emp, "me.png", "image/png", CONTENT,
                AttachmentOwnerType.EMPLOYEE_PROFILE, emp, "profile", 0, null);
        assertThat(uploaded.attachmentId()).isNotBlank();

        // 본인 다운로드 — 바이트 일치
        Download download = fileService.get(emp, uploaded.attachmentId());
        assertThat(download.content()).isEqualTo(CONTENT);

        // 타인 업로드 거부
        assertThatThrownBy(() -> fileService.upload(
                "other", "x.png", "image/png", CONTENT,
                AttachmentOwnerType.EMPLOYEE_PROFILE, emp, null, 0, null))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(FileErrorCode.ATTACHMENT_UPLOAD_FORBIDDEN);

        // 타인 다운로드 거부
        assertThatThrownBy(() -> fileService.get("other", uploaded.attachmentId()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(FileErrorCode.ATTACHMENT_READ_FORBIDDEN);

        // 본인 삭제 후 조회 불가
        fileService.delete(emp, uploaded.attachmentId());
        assertThatThrownBy(() -> fileService.get(emp, uploaded.attachmentId()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(FileErrorCode.ATTACHMENT_NOT_FOUND);
    }
}
