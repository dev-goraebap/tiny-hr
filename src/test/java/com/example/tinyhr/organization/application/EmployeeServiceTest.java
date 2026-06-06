package com.example.tinyhr.organization.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

import com.example.tinyhr.iam.application.AuthOpenHostService;
import com.example.tinyhr.organization.application.dto.CreateEmployeeRequest;
import com.example.tinyhr.organization.application.dto.UpdateEmployeeBasicRequest;
import com.example.tinyhr.organization.domain.OrganizationErrorCode;
import com.example.tinyhr.organization.domain.department.Department;
import com.example.tinyhr.organization.domain.department.DepartmentRepository;
import com.example.tinyhr.organization.domain.employee.Employee;
import com.example.tinyhr.organization.domain.employee.EmployeeRepository;
import com.example.tinyhr.organization.domain.employee.EmployeeStatus;
import com.example.tinyhr.organization.domain.rank.Rank;
import com.example.tinyhr.organization.domain.rank.RankRepository;
import com.example.tinyhr.shared.kernel.BusinessException;
import java.time.LocalDate;
import java.util.Optional;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    private static final LocalDate HIRE = LocalDate.of(2024, 1, 2);

    @Mock
    EmployeeRepository employeeRepository;

    @Mock
    DepartmentRepository departmentRepository;

    @Mock
    RankRepository rankRepository;

    @Mock
    AuthOpenHostService authOpenHostService;

    @InjectMocks
    EmployeeService employeeService;

    private static void assertBusiness(ThrowingCallable callable, OrganizationErrorCode expected) {
        assertThatThrownBy(callable)
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(expected);
    }

    private static CreateEmployeeRequest createRequest(String departmentId, String rankId) {
        return new CreateEmployeeRequest("홍길동", "user@example.com", departmentId, rankId, HIRE);
    }

    @Test
    @DisplayName("새 사원을 초대한다")
    void invite() {
        // given
        given(employeeRepository.existsByWorkEmailAndStatusIn(eq("user@example.com"), any()))
                .willReturn(false);
        Department dept = mock(Department.class);
        given(dept.isActive()).willReturn(true);
        given(departmentRepository.findById("dept1")).willReturn(Optional.of(dept));
        Rank rank = mock(Rank.class);
        given(rank.isActive()).willReturn(true);
        given(rankRepository.findById("rank1")).willReturn(Optional.of(rank));

        // when
        String id = employeeService.invite(createRequest("dept1", "rank1"));

        // then
        assertThat(id).isNotBlank();
        then(employeeRepository).should().save(any(Employee.class));
        then(authOpenHostService).should().provisionAccount(eq(id), eq("user@example.com"));
    }

    @Test
    @DisplayName("이미 인증 계정이 있는 이메일이면 초대할 수 없다")
    void rejectDuplicateAccountEmail() {
        given(employeeRepository.existsByWorkEmailAndStatusIn(eq("user@example.com"), any()))
                .willReturn(false);
        given(authOpenHostService.isEmailRegistered("user@example.com")).willReturn(true);

        assertBusiness(() -> employeeService.invite(createRequest(null, null)),
                OrganizationErrorCode.EMPLOYEE_EMAIL_DUPLICATED);
        then(employeeRepository).should(never()).save(any());
        then(authOpenHostService).should(never()).provisionAccount(any(), any());
    }

    @Test
    @DisplayName("업무 이메일이 중복되면 초대할 수 없다")
    void rejectDuplicateEmail() {
        given(employeeRepository.existsByWorkEmailAndStatusIn(eq("user@example.com"), any()))
                .willReturn(true);

        assertBusiness(() -> employeeService.invite(createRequest(null, null)),
                OrganizationErrorCode.EMPLOYEE_EMAIL_DUPLICATED);
        then(employeeRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("유효하지 않은 부서로는 초대할 수 없다")
    void rejectInvalidDepartment() {
        given(employeeRepository.existsByWorkEmailAndStatusIn(eq("user@example.com"), any()))
                .willReturn(false);
        given(departmentRepository.findById("dept1")).willReturn(Optional.empty());

        assertBusiness(() -> employeeService.invite(createRequest("dept1", null)),
                OrganizationErrorCode.DEPARTMENT_INVALID);
    }

    @Test
    @DisplayName("유효하지 않은 직급으로는 초대할 수 없다")
    void rejectInvalidRank() {
        given(employeeRepository.existsByWorkEmailAndStatusIn(eq("user@example.com"), any()))
                .willReturn(false);
        given(rankRepository.findById("rank1")).willReturn(Optional.empty());

        assertBusiness(() -> employeeService.invite(createRequest(null, "rank1")),
                OrganizationErrorCode.RANK_INVALID);
    }

    @Test
    @DisplayName("없는 사원은 활성화할 수 없다")
    void rejectActivateWhenNotFound() {
        given(employeeRepository.findById("none")).willReturn(Optional.empty());
        assertBusiness(() -> employeeService.activate("none"),
                OrganizationErrorCode.EMPLOYEE_NOT_FOUND);
    }

    @Test
    @DisplayName("재직 중이 아닌 사원은 부서를 해제할 수 없다")
    void rejectClearDepartmentWhenNotActive() {
        Employee invited = Employee.invite("홍길동", "user@example.com", "dept1", null, HIRE);
        given(employeeRepository.findById(invited.getId())).willReturn(Optional.of(invited));

        assertBusiness(() -> employeeService.clearDepartment(invited.getId()),
                OrganizationErrorCode.EMPLOYEE_NOT_ACTIVE);
    }

    @Test
    @DisplayName("기본 정보 수정 시 status=ACTIVE 를 주면 활성화된다")
    void updateBasicActivates() {
        Employee invited = Employee.invite("홍길동", "user@example.com", null, null, HIRE);
        given(employeeRepository.findById(invited.getId())).willReturn(Optional.of(invited));

        employeeService.updateBasic(invited.getId(),
                new UpdateEmployeeBasicRequest(null, null, null, null, null, EmployeeStatus.ACTIVE));

        assertThat(invited.getStatus()).isEqualTo(EmployeeStatus.ACTIVE);
        then(employeeRepository).should().save(invited);
    }
}
