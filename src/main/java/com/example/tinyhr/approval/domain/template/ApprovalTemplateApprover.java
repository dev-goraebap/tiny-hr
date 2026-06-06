package com.example.tinyhr.approval.domain.template;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 결재선 템플릿의 결재자 항목(값). order_no 1~3. */
@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ApprovalTemplateApprover {

    @Column(name = "employee_id", length = 36)
    private String employeeId;

    @Column(name = "order_no")
    private int orderNo;

    static ApprovalTemplateApprover of(String employeeId, int orderNo) {
        ApprovalTemplateApprover a = new ApprovalTemplateApprover();
        a.employeeId = employeeId;
        a.orderNo = orderNo;
        return a;
    }
}
