package com.example.tinyhr.file.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.example.tinyhr.file.application.FileService.Download;
import com.example.tinyhr.file.application.FileService.UploadResult;
import com.example.tinyhr.file.domain.AttachmentOwnerType;
import com.example.tinyhr.file.domain.FileErrorCode;
import com.example.tinyhr.file.domain.attachment.Attachment;
import com.example.tinyhr.file.domain.attachment.AttachmentRepository;
import com.example.tinyhr.file.domain.blob.Blob;
import com.example.tinyhr.file.domain.blob.BlobRepository;
import com.example.tinyhr.file.domain.blob.BlobStorage;
import com.example.tinyhr.shared.kernel.BusinessException;
import java.util.Optional;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FileServiceTest {

    @Mock AttachmentRepository attachmentRepository;
    @Mock BlobRepository blobRepository;
    @Mock BlobStorage blobStorage;
    @Mock AttachmentAccessService accessService;

    @InjectMocks FileService fileService;

    private static final byte[] BODY = "hello".getBytes();
    private static final AttachmentOwnerType OWNER = AttachmentOwnerType.EMPLOYEE_PROFILE;

    private static void assertBusiness(ThrowingCallable callable, FileErrorCode expected) {
        assertThatThrownBy(callable)
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(expected);
    }

    private UploadResult upload(String actor) {
        return fileService.upload(actor, "a.png", "image/png", BODY, OWNER, "emp", "profile", 0, null);
    }

    @Test
    @DisplayName("빈 파일은 업로드할 수 없다")
    void upload_empty() {
        assertBusiness(
                () -> fileService.upload("emp", "a.png", "image/png", new byte[0], OWNER, "emp", null, 0, null),
                FileErrorCode.EMPTY_FILE);
    }

    @Test
    @DisplayName("업로드 권한이 없으면 거부된다")
    void upload_forbidden() {
        given(accessService.canUpload("other", OWNER, "emp")).willReturn(false);

        assertBusiness(() -> upload("other"), FileErrorCode.ATTACHMENT_UPLOAD_FORBIDDEN);
        then(attachmentRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("새 파일이면 blob 을 저장하고 첨부를 만든다")
    void upload_newBlob() {
        given(accessService.canUpload("emp", OWNER, "emp")).willReturn(true);
        given(blobRepository.findByChecksum(anyString())).willReturn(Optional.empty());

        UploadResult result = upload("emp");

        assertThat(result.attachmentId()).isNotBlank();
        then(blobStorage).should().store(anyString(), any());
        then(blobRepository).should().save(any(Blob.class));
        then(attachmentRepository).should().save(any(Attachment.class));
    }

    @Test
    @DisplayName("같은 체크섬이면 기존 blob 을 재사용한다")
    void upload_dedupe() {
        given(accessService.canUpload("emp", OWNER, "emp")).willReturn(true);
        Blob existing = Blob.create("key", "a.png", "image/png", 5, "b".repeat(64));
        given(blobRepository.findByChecksum(anyString())).willReturn(Optional.of(existing));

        upload("emp");

        then(blobStorage).should(never()).store(anyString(), any());
        then(blobRepository).should(never()).save(any());
        then(attachmentRepository).should().save(any(Attachment.class));
    }

    @Test
    @DisplayName("조회 권한이 없으면 거부된다")
    void get_forbidden() {
        Attachment a = Attachment.create("blob-1", OWNER, "emp", null, 0, null, "emp");
        given(attachmentRepository.findById("att-1")).willReturn(Optional.of(a));
        given(accessService.canRead("other", a)).willReturn(false);

        assertBusiness(() -> fileService.get("other", "att-1"),
                FileErrorCode.ATTACHMENT_READ_FORBIDDEN);
    }

    @Test
    @DisplayName("조회 권한이 있으면 메타와 바이트를 돌려준다")
    void get_success() {
        Attachment a = Attachment.create("blob-1", OWNER, "emp", null, 0, null, "emp");
        Blob blob = Blob.create("key/1", "a.png", "image/png", 5, "c".repeat(64));
        given(attachmentRepository.findById("att-1")).willReturn(Optional.of(a));
        given(accessService.canRead("emp", a)).willReturn(true);
        given(blobRepository.findById("blob-1")).willReturn(Optional.of(blob));
        given(blobStorage.load("key/1")).willReturn(BODY);

        Download download = fileService.get("emp", "att-1");

        assertThat(download.filename()).isEqualTo("a.png");
        assertThat(download.content()).isEqualTo(BODY);
    }

    @Test
    @DisplayName("삭제 권한이 있으면 소프트 삭제한다")
    void delete_success() {
        Attachment a = Attachment.create("blob-1", OWNER, "emp", null, 0, null, "emp");
        given(attachmentRepository.findById("att-1")).willReturn(Optional.of(a));
        given(accessService.canDelete("emp", a)).willReturn(true);

        fileService.delete("emp", "att-1");

        assertThat(a.isDeleted()).isTrue();
        then(attachmentRepository).should().save(a);
    }
}
