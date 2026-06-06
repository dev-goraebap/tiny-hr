package com.example.tinyhr.approval.domain.template;

import com.example.tinyhr.approval.domain.ApprovalErrorCode;
import com.example.tinyhr.approval.domain.ApprovalLineCategory;
import com.example.tinyhr.shared.kernel.BusinessException;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 부서 × 카테고리 단위 결재선 애그리거트 루트.
 *
 * <p>불변식: 결재자 1~3명, 순서(order_no) 1~3 중복 없음, 같은 사원 중복 없음. 한 부서에서 카테고리당
 * 1개만 존재한다(유니크).
 */
@Entity
@Table(name = "approval_template",
        uniqueConstraints = @UniqueConstraint(columnNames = {"department_id", "category"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ApprovalTemplate {

    private static final int MAX_APPROVERS = 3;

    @Id
    @Column(name = "approval_template_id", length = 36)
    private String id;

    @Column(name = "department_id", nullable = false, length = 36)
    private String departmentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ApprovalLineCategory category;

    @ElementCollection
    @CollectionTable(name = "approval_template_approver",
            joinColumns = @JoinColumn(name = "approval_template_id"))
    private List<ApprovalTemplateApprover> approvers = new ArrayList<>();

    /** 신규 결재선 생성. 카테고리·부서는 이후 변경 불가. */
    public static ApprovalTemplate create(
            String departmentId,
            ApprovalLineCategory category,
            List<ApproverInput> approvers) {
        ApprovalTemplate t = new ApprovalTemplate();
        t.id = UUID.randomUUID().toString();
        t.departmentId = departmentId;
        t.category = category;
        t.approvers = validateAndBuild(approvers);
        return t;
    }

    /** 결재자 전체 교체. 부서·카테고리는 유지. */
    public void replaceApprovers(List<ApproverInput> approvers) {
        this.approvers = validateAndBuild(approvers);
    }

    public List<ApprovalTemplateApprover> getApprovers() {
        return Collections.unmodifiableList(approvers);
    }

    private static List<ApprovalTemplateApprover> validateAndBuild(List<ApproverInput> approvers) {
        if (approvers == null || approvers.isEmpty() || approvers.size() > MAX_APPROVERS) {
            throw new BusinessException(ApprovalErrorCode.APPROVAL_TEMPLATE_INVALID_APPROVERS);
        }
        long distinctOrders = approvers.stream().map(ApproverInput::orderNo).distinct().count();
        long distinctEmployees =
                approvers.stream().map(ApproverInput::employeeId).distinct().count();
        boolean orderInRange =
                approvers.stream().allMatch(a -> a.orderNo() >= 1 && a.orderNo() <= MAX_APPROVERS);
        if (distinctOrders != approvers.size()
                || distinctEmployees != approvers.size()
                || !orderInRange) {
            throw new BusinessException(ApprovalErrorCode.APPROVAL_TEMPLATE_INVALID_APPROVERS);
        }
        return approvers.stream()
                .sorted(Comparator.comparingInt(ApproverInput::orderNo))
                .map(a -> ApprovalTemplateApprover.of(a.employeeId(), a.orderNo()))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    /** 결재자 입력(도메인/애플리케이션 내부 Props). */
    public record ApproverInput(String employeeId, int orderNo) {}
}
