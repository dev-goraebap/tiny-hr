package com.example.tinyhr.organization.domain.position;

import com.example.tinyhr.shared.kernel.BadRequestException;

/** 재정렬 입력에 같은 직위 ID 가 중복됨. */
public class PositionReorderDuplicateIdException extends BadRequestException {
    public PositionReorderDuplicateIdException() {
        super("POSITION_REORDER_DUPLICATE_ID: positionId 중복 금지");
    }
}
