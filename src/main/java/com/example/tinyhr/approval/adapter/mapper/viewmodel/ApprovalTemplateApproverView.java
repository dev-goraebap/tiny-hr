package com.example.tinyhr.approval.adapter.mapper.viewmodel;

import lombok.Getter;
import lombok.Setter;

/** 결재선 결재자 조회 뷰. 중첩 컬렉션 매핑을 위해 setter 기반 POJO. */
@Getter
@Setter
public class ApprovalTemplateApproverView {

    private String employeeId;
    private int orderNo;
}
