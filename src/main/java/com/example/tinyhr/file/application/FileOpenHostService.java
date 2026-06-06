package com.example.tinyhr.file.application;

import com.example.tinyhr.file.domain.AttachmentOwnerType;
import com.example.tinyhr.file.domain.FileErrorCode;
import com.example.tinyhr.file.domain.attachment.Attachment;
import com.example.tinyhr.file.domain.attachment.AttachmentRepository;
import com.example.tinyhr.shared.kernel.BusinessException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * file 컨텍스트가 외부에 공개하는 Open Host Service.
 *
 * <p>다른 컨텍스트(vacation 등)가 사전 업로드한 첨부를 새 엔티티 식별자로 이관할 때 쓴다. 첨부
 * 불변식(존재·소유타입·업로더 본인)은 file 이 소유한다. 호출자의 트랜잭션 안에서 함께 부른다.
 */
@Service
@Transactional
public class FileOpenHostService {

    private final AttachmentRepository attachmentRepository;

    public FileOpenHostService(AttachmentRepository attachmentRepository) {
        this.attachmentRepository = attachmentRepository;
    }

    /**
     * 사전 업로드된 첨부들의 owner 를 새 id 로 이관한다. 각 첨부가 (1) 존재·미회수, (2) 기대 ownerType
     * 일치, (3) requester 본인 업로드임을 검증한 뒤 이관한다.
     */
    public void transferOwnership(
            List<String> attachmentIds,
            AttachmentOwnerType expectedOwnerType,
            String requesterId,
            String newOwnerId) {
        for (String attachmentId : attachmentIds) {
            Attachment attachment = attachmentRepository.findById(attachmentId)
                    .filter(a -> !a.isDeleted())
                    .orElseThrow(() ->
                            new BusinessException(FileErrorCode.ATTACHMENT_TRANSFER_FORBIDDEN));
            if (attachment.getOwnerType() != expectedOwnerType
                    || !requesterId.equals(attachment.getCreatedBy())) {
                throw new BusinessException(FileErrorCode.ATTACHMENT_TRANSFER_FORBIDDEN);
            }
            attachment.transferOwnerTo(newOwnerId);
            attachmentRepository.save(attachment);
        }
    }
}
