package com.example.tinyhr.file.adapter.web;

import com.example.tinyhr.file.application.FileService;
import com.example.tinyhr.file.application.FileService.Download;
import com.example.tinyhr.file.application.FileService.UploadResult;
import com.example.tinyhr.file.domain.AttachmentOwnerType;
import com.example.tinyhr.file.domain.FileErrorCode;
import com.example.tinyhr.iam.adapter.security.AuthPrincipal;
import com.example.tinyhr.shared.kernel.ApiResponse;
import com.example.tinyhr.shared.kernel.BusinessException;
import java.io.IOException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 첨부 업로드·다운로드·삭제 HTTP 진입점. {@code /attachments/**} 는 인증 필요(SecurityConfig).
 * 실제 인가는 owner 별 {@code AttachmentAccessSpi} 가 판정한다.
 */
@RestController
@RequestMapping("/attachments")
public class AttachmentController {

    private final FileService fileService;

    public AttachmentController(FileService fileService) {
        this.fileService = fileService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<UploadResult> upload(
            @AuthenticationPrincipal AuthPrincipal principal,
            @RequestParam("file") MultipartFile file,
            @RequestParam("ownerType") AttachmentOwnerType ownerType,
            @RequestParam("ownerId") String ownerId,
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "position", defaultValue = "0") int position,
            @RequestParam(value = "note", required = false) String note) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(FileErrorCode.FILE_REQUIRED);
        }
        UploadResult result = fileService.upload(
                principal.userAccountId(),
                file.getOriginalFilename(),
                file.getContentType(),
                readBytes(file),
                ownerType, ownerId, name, position, note);
        return ApiResponse.of(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<byte[]> download(
            @AuthenticationPrincipal AuthPrincipal principal, @PathVariable String id) {
        Download download = fileService.get(principal.userAccountId(), id);
        MediaType mediaType = download.contentType() == null
                ? MediaType.APPLICATION_OCTET_STREAM
                : MediaType.parseMediaType(download.contentType());
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + download.filename() + "\"")
                .body(download.content());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @AuthenticationPrincipal AuthPrincipal principal, @PathVariable String id) {
        fileService.delete(principal.userAccountId(), id);
    }

    private static byte[] readBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new BusinessException(FileErrorCode.FILE_REQUIRED);
        }
    }
}
