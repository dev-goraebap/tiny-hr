package com.example.tinyhr.organization.application;

import com.example.tinyhr.organization.application.dto.CreateEmployeeRequest;
import com.example.tinyhr.organization.application.dto.UpdateEmployeeBasicRequest;
import com.example.tinyhr.organization.application.dto.UpdateEmployeeRequest;
import com.example.tinyhr.organization.domain.OrganizationErrorCode;
import com.example.tinyhr.organization.domain.department.DepartmentRepository;
import com.example.tinyhr.organization.domain.employee.Employee;
import com.example.tinyhr.organization.domain.employee.EmployeeRepository;
import com.example.tinyhr.organization.domain.employee.EmployeeStatus;
import com.example.tinyhr.organization.domain.rank.RankRepository;
import com.example.tinyhr.shared.kernel.BusinessException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 사원 라이프사이클(초대·활성화·퇴직)과 조직 배치·기본 정보를 관리한다.
 *
 * <p>참고 프로젝트 단순화(MVP): 계정 연동·프로필·부서장 무결성·직위는 다루지 않는다.
 *
 * @actor 관리자
 */
@Service
@Transactional
public class EmployeeService {

    private static final List<EmployeeStatus> ACTIVE_OR_INVITED =
            List.of(EmployeeStatus.ACTIVE, EmployeeStatus.INVITED);

    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final RankRepository rankRepository;

    public EmployeeService(
            EmployeeRepository employeeRepository,
            DepartmentRepository departmentRepository,
            RankRepository rankRepository) {
        this.employeeRepository = employeeRepository;
        this.departmentRepository = departmentRepository;
        this.rankRepository = rankRepository;
    }

    /** 새 사원 초대. 업무 이메일 중복과 부서·직급 유효성을 검증한 뒤 INVITED 로 생성한다. */
    public String invite(CreateEmployeeRequest request) {
        String workEmail = Employee.normalizeWorkEmail(request.workEmail());
        if (employeeRepository.existsByWorkEmailAndStatusIn(workEmail, ACTIVE_OR_INVITED)) {
            throw new BusinessException(OrganizationErrorCode.EMPLOYEE_EMAIL_DUPLICATED);
        }
        validateDepartment(request.departmentId());
        validateRank(request.rankId());

        Employee employee = Employee.invite(
                request.name(),
                request.workEmail(),
                request.departmentId(),
                request.rankId(),
                request.hireDate());
        employeeRepository.save(employee);
        return employee.getId();
    }

    /** 초대 상태 사원을 재직(활성)으로 전환. */
    public void activate(String employeeId) {
        Employee employee = findOrThrow(employeeId);
        employee.activate();
        employeeRepository.save(employee);
    }

    /** 사원을 퇴직 처리. */
    public void terminate(String employeeId) {
        Employee employee = findOrThrow(employeeId);
        employee.terminate();
        employeeRepository.save(employee);
    }

    /** 조직 배치(부서·직급)와 기본 정보(이름·업무 이메일) 수정. null 필드는 변경하지 않는다. */
    public void update(String employeeId, UpdateEmployeeRequest request) {
        Employee employee = findOrThrow(employeeId);
        if (request.departmentId() != null) {
            validateDepartment(request.departmentId());
        }
        if (request.rankId() != null) {
            validateRank(request.rankId());
        }
        employee.updateOrganizationAssignment(request.departmentId(), request.rankId());
        employee.updateProfileByAdmin(request.name(), request.workEmail());
        employeeRepository.save(employee);
    }

    /** 기본 정보 일괄 수정(이름·이메일·직급·입사일·생년월일) + status 전이. */
    public void updateBasic(String employeeId, UpdateEmployeeBasicRequest request) {
        Employee employee = findOrThrow(employeeId);
        if (request.rankId() != null) {
            validateRank(request.rankId());
        }
        employee.updateOrganizationAssignment(null, request.rankId());
        employee.updateProfileByAdmin(request.name(), request.workEmail());
        employee.updateBasicByAdmin(request.hireDate(), request.birthDate());
        applyStatusTransition(employee, request.status());
        employeeRepository.save(employee);
    }

    /** 부서 소속 해제. 재직 중인 사원만 가능. */
    public void clearDepartment(String employeeId) {
        Employee employee = findOrThrow(employeeId);
        if (employee.getStatus() != EmployeeStatus.ACTIVE) {
            throw new BusinessException(OrganizationErrorCode.EMPLOYEE_NOT_ACTIVE);
        }
        employee.clearDepartment();
        employeeRepository.save(employee);
    }

    private void applyStatusTransition(Employee employee, EmployeeStatus target) {
        if (target == EmployeeStatus.ACTIVE && employee.getStatus() == EmployeeStatus.INVITED) {
            employee.activate();
        } else if (target == EmployeeStatus.TERMINATED
                && employee.getStatus() != EmployeeStatus.TERMINATED) {
            employee.terminate();
        }
    }

    private Employee findOrThrow(String employeeId) {
        return employeeRepository.findById(employeeId)
                .orElseThrow(() -> new BusinessException(OrganizationErrorCode.EMPLOYEE_NOT_FOUND));
    }

    private void validateDepartment(String departmentId) {
        if (departmentId == null) {
            return;
        }
        boolean ok = departmentRepository.findById(departmentId)
                .map(d -> d.isActive())
                .orElse(false);
        if (!ok) {
            throw new BusinessException(OrganizationErrorCode.DEPARTMENT_INVALID);
        }
    }

    private void validateRank(String rankId) {
        if (rankId == null) {
            return;
        }
        boolean ok = rankRepository.findById(rankId)
                .map(r -> r.isActive())
                .orElse(false);
        if (!ok) {
            throw new BusinessException(OrganizationErrorCode.RANK_INVALID);
        }
    }
}
