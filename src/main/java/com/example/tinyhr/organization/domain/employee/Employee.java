package com.example.tinyhr.organization.domain.employee;

import com.example.tinyhr.organization.domain.OrganizationErrorCode;
import com.example.tinyhr.shared.kernel.BusinessException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 사원 애그리거트 루트.
 *
 * <p>라이프사이클: {@link #invite}(INVITED) → {@link #activate}(ACTIVE) → {@link #terminate}(TERMINATED).
 * 내부 상태는 private, 변경은 도메인 메서드로만 한다(setter 없음).
 *
 * <p>참고 프로젝트 단순화(MVP): 직위(title)·계정 연동·프로필·부서장 무결성은 다루지 않는다.
 * 부서/직급 변경은 "값이 있으면 설정, null 이면 변경 없음"으로 처리하고, 부서 해제만 별도
 * {@link #clearDepartment} 로 둔다.
 */
@Entity
@Table(name = "employee")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Employee {

    @Id
    @Column(name = "employee_id", length = 36)
    private String id;

    @Column(nullable = false, length = 64)
    private String name;

    /** 업무 이메일. lowercase 정규화. 사내 유일(서비스에서 검증). */
    @Column(name = "work_email", nullable = false)
    private String workEmail;

    /** 소속 부서 ID. null = 미배정. */
    @Column(name = "department_id", length = 36)
    private String departmentId;

    /** 직급 ID. null = 미지정. */
    @Column(name = "rank_id", length = 36)
    private String rankId;

    @Column(name = "hire_date", nullable = false)
    private LocalDate hireDate;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EmployeeStatus status;

    @Column(name = "invited_at", nullable = false)
    private Instant invitedAt;

    @Column(name = "activated_at")
    private Instant activatedAt;

    @Column(name = "terminated_at")
    private Instant terminatedAt;

    /** 신규 사원 초대 — INVITED 상태로 최초 생성. 식별자는 도메인이 발급한다. */
    public static Employee invite(
            String name, String workEmail, String departmentId, String rankId, LocalDate hireDate) {
        Employee e = new Employee();
        e.id = UUID.randomUUID().toString();
        e.name = normalizeName(name);
        e.workEmail = normalizeWorkEmail(workEmail);
        e.departmentId = departmentId;
        e.rankId = rankId;
        e.hireDate = hireDate;
        e.birthDate = null;
        e.status = EmployeeStatus.INVITED;
        e.invitedAt = Instant.now();
        e.activatedAt = null;
        e.terminatedAt = null;
        return e;
    }

    /** 초대 상태의 사원을 재직(활성)으로 전환. */
    public void activate() {
        if (status == EmployeeStatus.ACTIVE) {
            throw new BusinessException(OrganizationErrorCode.EMPLOYEE_ALREADY_ACTIVE);
        }
        if (status != EmployeeStatus.INVITED) {
            throw new BusinessException(OrganizationErrorCode.EMPLOYEE_INVALID_STATUS_TRANSITION);
        }
        status = EmployeeStatus.ACTIVE;
        activatedAt = Instant.now();
    }

    /** 퇴직 처리. */
    public void terminate() {
        if (status == EmployeeStatus.TERMINATED) {
            throw new BusinessException(OrganizationErrorCode.EMPLOYEE_ALREADY_TERMINATED);
        }
        status = EmployeeStatus.TERMINATED;
        terminatedAt = Instant.now();
    }

    /** 부서·직급 배정 변경. null 인자는 "변경 없음". 부서 해제는 {@link #clearDepartment}. */
    public void updateOrganizationAssignment(String departmentId, String rankId) {
        ensureEditable();
        if (departmentId != null) {
            this.departmentId = departmentId;
        }
        if (rankId != null) {
            this.rankId = rankId;
        }
    }

    /** 관리자의 이름·업무 이메일 편집. null 인자는 "변경 없음". */
    public void updateProfileByAdmin(String name, String workEmail) {
        ensureEditable();
        if (name != null) {
            this.name = normalizeName(name);
        }
        if (workEmail != null) {
            this.workEmail = normalizeWorkEmail(workEmail);
        }
    }

    /** 관리자의 입사일·생년월일 편집. null 인자는 "변경 없음". */
    public void updateBasicByAdmin(LocalDate hireDate, LocalDate birthDate) {
        ensureEditable();
        if (hireDate != null) {
            this.hireDate = hireDate;
        }
        if (birthDate != null) {
            this.birthDate = birthDate;
        }
    }

    /** 부서 소속 해제(미배정). 이미 미배정이면 아무것도 하지 않는다. */
    public void clearDepartment() {
        ensureEditable();
        this.departmentId = null;
    }

    private void ensureEditable() {
        if (status == EmployeeStatus.TERMINATED) {
            throw new BusinessException(OrganizationErrorCode.EMPLOYEE_TERMINATED_READ_ONLY);
        }
    }

    /** 사원 이름 정규화 — trim, 1~64자. */
    private static String normalizeName(String raw) {
        if (raw == null) {
            throw new BusinessException(OrganizationErrorCode.EMPLOYEE_NAME_INVALID);
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty() || trimmed.length() > 64) {
            throw new BusinessException(OrganizationErrorCode.EMPLOYEE_NAME_INVALID);
        }
        return trimmed;
    }

    /** 업무 이메일 정규화 — trim, lowercase + 형식 검증. 중복 검사에 재사용하도록 공개. */
    public static String normalizeWorkEmail(String raw) {
        if (raw == null) {
            throw new BusinessException(OrganizationErrorCode.EMPLOYEE_WORK_EMAIL_INVALID);
        }
        String normalized = raw.trim().toLowerCase();
        if (!isValidEmailFormat(normalized)) {
            throw new BusinessException(OrganizationErrorCode.EMPLOYEE_WORK_EMAIL_INVALID);
        }
        return normalized;
    }

    private static boolean isValidEmailFormat(String email) {
        if (email.length() < 3 || email.length() > 254) {
            return false;
        }
        int at = email.indexOf('@');
        if (at < 1 || at > email.length() - 2) {
            return false;
        }
        if (email.indexOf('@', at + 1) != -1) {
            return false;
        }
        String domain = email.substring(at + 1);
        return domain.contains(".");
    }
}
