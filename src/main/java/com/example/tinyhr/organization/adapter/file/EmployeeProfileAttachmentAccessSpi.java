package com.example.tinyhr.organization.adapter.file;

import com.example.tinyhr.file.application.spi.AttachmentAccessSpi;
import com.example.tinyhr.file.domain.AttachmentOwnerType;
import org.springframework.stereotype.Component;

/**
 * 사원 프로필 첨부(EMPLOYEE_PROFILE)의 접근 인가 — organization 이 소유하는 규칙.
 *
 * <p>file 의 {@link AttachmentAccessSpi} 를 organization 에서 구현(@Component)해 file 레지스트리에
 * 자동 등록한다(의존 방향 organization → file 단방향, 제어 흐름만 file → organization 으로 역전).
 *
 * <p>MVP 규칙: 사원 본인(actor == ownerId == employeeId)만 자기 프로필 첨부를 업로드·조회·삭제할 수
 * 있다. (관리자 허용 등은 RBAC 연동 시 확장)
 */
@Component
public class EmployeeProfileAttachmentAccessSpi implements AttachmentAccessSpi {

    @Override
    public AttachmentOwnerType ownerType() {
        return AttachmentOwnerType.EMPLOYEE_PROFILE;
    }

    @Override
    public boolean canUpload(UploadContext ctx) {
        return ctx.actorId().equals(ctx.ownerId());
    }

    @Override
    public boolean canRead(ResourceContext ctx) {
        return ctx.actorId().equals(ctx.ownerId());
    }

    @Override
    public boolean canDelete(ResourceContext ctx) {
        return ctx.actorId().equals(ctx.ownerId());
    }
}
