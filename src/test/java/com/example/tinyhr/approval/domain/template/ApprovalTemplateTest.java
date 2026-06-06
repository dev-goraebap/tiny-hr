package com.example.tinyhr.approval.domain.template;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.tinyhr.approval.domain.ApprovalErrorCode;
import com.example.tinyhr.approval.domain.ApprovalLineCategory;
import com.example.tinyhr.approval.domain.template.ApprovalTemplate.ApproverInput;
import com.example.tinyhr.shared.kernel.BusinessException;
import java.util.List;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ApprovalTemplateTest {

    private static void assertInvalid(ThrowingCallable callable) {
        assertThatThrownBy(callable)
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ApprovalErrorCode.APPROVAL_TEMPLATE_INVALID_APPROVERS);
    }

    private static ApprovalTemplate create(List<ApproverInput> approvers) {
        return ApprovalTemplate.create("dept-1", ApprovalLineCategory.ANNUAL, approvers);
    }

    @Test
    @DisplayName("결재자를 순서대로 정렬해 생성한다")
    void create() {
        ApprovalTemplate t = create(List.of(new ApproverInput("e2", 2), new ApproverInput("e1", 1)));

        assertThat(t.getId()).isNotBlank();
        assertThat(t.getApprovers())
                .extracting(ApprovalTemplateApprover::getEmployeeId)
                .containsExactly("e1", "e2");
    }

    @Test
    @DisplayName("결재자가 없으면 생성할 수 없다")
    void create_empty() {
        assertInvalid(() -> create(List.of()));
    }

    @Test
    @DisplayName("결재자가 4명 이상이면 생성할 수 없다")
    void create_tooMany() {
        assertInvalid(() -> create(List.of(
                new ApproverInput("e1", 1), new ApproverInput("e2", 2),
                new ApproverInput("e3", 3), new ApproverInput("e4", 1))));
    }

    @Test
    @DisplayName("순서가 중복되면 생성할 수 없다")
    void create_duplicateOrder() {
        assertInvalid(() -> create(List.of(new ApproverInput("e1", 1), new ApproverInput("e2", 1))));
    }

    @Test
    @DisplayName("같은 사원이 중복되면 생성할 수 없다")
    void create_duplicateEmployee() {
        assertInvalid(() -> create(List.of(new ApproverInput("e1", 1), new ApproverInput("e1", 2))));
    }

    @Test
    @DisplayName("순서가 범위를 벗어나면 생성할 수 없다")
    void create_orderOutOfRange() {
        assertInvalid(() -> create(List.of(new ApproverInput("e1", 0))));
    }

    @Test
    @DisplayName("결재자를 교체할 수 있다")
    void replaceApprovers() {
        ApprovalTemplate t = create(List.of(new ApproverInput("e1", 1)));

        t.replaceApprovers(List.of(new ApproverInput("x1", 1), new ApproverInput("x2", 2)));

        assertThat(t.getApprovers())
                .extracting(ApprovalTemplateApprover::getEmployeeId)
                .containsExactly("x1", "x2");
    }
}
