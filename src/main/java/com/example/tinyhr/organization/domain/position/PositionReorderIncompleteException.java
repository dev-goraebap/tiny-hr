package com.example.tinyhr.organization.domain.position;

import com.example.tinyhr.shared.kernel.BadRequestException;

/** 재정렬 입력이 활성 직위 전체를 빠짐없이 포함하지 않음. */
public class PositionReorderIncompleteException extends BadRequestException {
    public PositionReorderIncompleteException() {
        this("활성 직위 전체가 재정렬 입력에 포함되어야 합니다");
    }

    public PositionReorderIncompleteException(String detail) {
        super("POSITION_REORDER_INCOMPLETE: " + detail);
    }
}
