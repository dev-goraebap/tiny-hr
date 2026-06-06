package com.example.tinyhr.organization.domain.employee;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.tinyhr.organization.domain.OrganizationErrorCode;
import com.example.tinyhr.shared.kernel.BusinessException;
import java.time.LocalDate;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class EmployeeTest {

    private static final LocalDate HIRE = LocalDate.of(2024, 1, 2);

    private static void assertBusiness(ThrowingCallable callable, OrganizationErrorCode expected) {
        assertThatThrownBy(callable)
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(expected);
    }

    private static Employee invited() {
        return Employee.invite("홍길동", "user@example.com", "dept1", "rank1", HIRE);
    }

    @Test
    @DisplayName("새 사원은 입력을 정규화해 INVITED 로 초대된다")
    void invite() {
        // when
        Employee e = Employee.invite("  홍길동  ", "User@Example.COM", "dept1", "rank1", HIRE);

        // then
        assertThat(e.getId()).isNotBlank();
        assertThat(e.getName()).isEqualTo("홍길동");
        assertThat(e.getWorkEmail()).isEqualTo("user@example.com");
        assertThat(e.getDepartmentId()).isEqualTo("dept1");
        assertThat(e.getRankId()).isEqualTo("rank1");
        assertThat(e.getStatus()).isEqualTo(EmployeeStatus.INVITED);
        assertThat(e.getInvitedAt()).isNotNull();
        assertThat(e.getActivatedAt()).isNull();
    }

    @Test
    @DisplayName("이름이 비어 있으면 초대할 수 없다")
    void rejectBlankName() {
        assertBusiness(() -> Employee.invite("   ", "user@example.com", null, null, HIRE),
                OrganizationErrorCode.EMPLOYEE_NAME_INVALID);
    }

    @Test
    @DisplayName("업무 이메일 형식이 잘못되면 초대할 수 없다")
    void rejectInvalidEmail() {
        assertBusiness(() -> Employee.invite("홍길동", "not-an-email", null, null, HIRE),
                OrganizationErrorCode.EMPLOYEE_WORK_EMAIL_INVALID);
    }

    @Test
    @DisplayName("초대 사원을 활성화하면 ACTIVE 가 된다")
    void activate() {
        // given
        Employee e = invited();

        // when
        e.activate();

        // then
        assertThat(e.getStatus()).isEqualTo(EmployeeStatus.ACTIVE);
        assertThat(e.getActivatedAt()).isNotNull();
    }

    @Test
    @DisplayName("이미 활성화된 사원은 다시 활성화할 수 없다")
    void rejectActivateWhenAlreadyActive() {
        Employee e = invited();
        e.activate();
        assertBusiness(e::activate, OrganizationErrorCode.EMPLOYEE_ALREADY_ACTIVE);
    }

    @Test
    @DisplayName("퇴직한 사원은 활성화할 수 없다")
    void rejectActivateWhenTerminated() {
        Employee e = invited();
        e.terminate();
        assertBusiness(e::activate, OrganizationErrorCode.EMPLOYEE_INVALID_STATUS_TRANSITION);
    }

    @Test
    @DisplayName("사원을 퇴직 처리하면 TERMINATED 가 된다")
    void terminate() {
        Employee e = invited();
        e.terminate();
        assertThat(e.getStatus()).isEqualTo(EmployeeStatus.TERMINATED);
        assertThat(e.getTerminatedAt()).isNotNull();
    }

    @Test
    @DisplayName("이미 퇴직한 사원은 다시 퇴직 처리할 수 없다")
    void rejectTerminateWhenAlreadyTerminated() {
        Employee e = invited();
        e.terminate();
        assertBusiness(e::terminate, OrganizationErrorCode.EMPLOYEE_ALREADY_TERMINATED);
    }

    @Test
    @DisplayName("부서·직급 배정 변경 시 null 인자는 변경하지 않는다")
    void updateOrganizationAssignmentSkipsNull() {
        Employee e = invited();
        e.updateOrganizationAssignment(null, "rank2");
        assertThat(e.getDepartmentId()).isEqualTo("dept1");
        assertThat(e.getRankId()).isEqualTo("rank2");
    }

    @Test
    @DisplayName("부서를 해제하면 미배정이 된다")
    void clearDepartment() {
        Employee e = invited();
        e.clearDepartment();
        assertThat(e.getDepartmentId()).isNull();
    }

    @Test
    @DisplayName("퇴직한 사원은 수정할 수 없다")
    void rejectEditWhenTerminated() {
        Employee e = invited();
        e.terminate();
        assertBusiness(() -> e.updateOrganizationAssignment("dept2", null),
                OrganizationErrorCode.EMPLOYEE_TERMINATED_READ_ONLY);
    }
}
