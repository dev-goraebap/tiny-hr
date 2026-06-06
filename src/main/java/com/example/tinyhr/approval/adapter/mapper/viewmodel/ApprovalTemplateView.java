package com.example.tinyhr.approval.adapter.mapper.viewmodel;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

/**
 * 결재선 템플릿 조회 뷰(읽기 전용). approvers 는 자식 테이블을 묶은 중첩 컬렉션이라
 * MyBatis {@code <collection>} 매핑이 필요해 setter 기반 POJO 로 둔다(RoleListItem 과 동일 사유).
 */
@Getter
@Setter
public class ApprovalTemplateView {

    private String approvalTemplateId;
    private String departmentId;
    private String category;
    private List<ApprovalTemplateApproverView> approvers = new ArrayList<>();
}
