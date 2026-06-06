package com.example.tinyhr.iam.application;

import com.example.tinyhr.iam.application.dto.AssignRoleRequest;
import com.example.tinyhr.iam.domain.IamErrorCode;
import com.example.tinyhr.iam.domain.rbac.Permission;
import com.example.tinyhr.iam.domain.role.Role;
import com.example.tinyhr.iam.domain.role.RoleManagerCounter;
import com.example.tinyhr.iam.domain.role.RoleRepository;
import com.example.tinyhr.iam.domain.roleassignment.RoleAssignment;
import com.example.tinyhr.iam.domain.roleassignment.RoleAssignmentRepository;
import com.example.tinyhr.iam.domain.useraccount.UserAccountRepository;
import com.example.tinyhr.shared.kernel.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 역할 부여·회수 오케스트레이션.
 *
 * <p>부여 대상 사원의 존재는 iam 이 소유한 인증 계정({@code UserAccount}, id == employeeId)으로
 * 확인한다(다른 BC 테이블을 직접 만지지 않는다). 자기 잠금(self-lockout) 방어 — 활성
 * {@code ROLE_MANAGE} 보유자가 0 이 되는 회수는 거부한다.
 *
 * @actor 관리자
 */
@Service
@Transactional
public class RoleAssignmentService {

    private final UserAccountRepository userAccountRepository;
    private final RoleRepository roleRepository;
    private final RoleAssignmentRepository roleAssignmentRepository;
    private final RoleManagerCounter roleManagerCounter;

    public RoleAssignmentService(
            UserAccountRepository userAccountRepository,
            RoleRepository roleRepository,
            RoleAssignmentRepository roleAssignmentRepository,
            RoleManagerCounter roleManagerCounter) {
        this.userAccountRepository = userAccountRepository;
        this.roleRepository = roleRepository;
        this.roleAssignmentRepository = roleAssignmentRepository;
        this.roleManagerCounter = roleManagerCounter;
    }

    /** 사원에게 역할을 부여한다. 활성 역할만, 같은 부여가 활성으로 중복되면 거부. */
    public String assign(AssignRoleRequest request) {
        if (!userAccountRepository.existsById(request.userAccountId())) {
            throw new BusinessException(IamErrorCode.ROLE_ASSIGNMENT_USER_NOT_FOUND);
        }
        Role role = roleRepository.findById(request.roleId())
                .orElseThrow(() -> new BusinessException(IamErrorCode.ROLE_NOT_FOUND));
        if (!role.isActive()) {
            throw new BusinessException(IamErrorCode.ROLE_INACTIVE);
        }
        roleAssignmentRepository
                .findByUserAccountIdAndRoleIdAndRevokedAtIsNull(request.userAccountId(), request.roleId())
                .ifPresent(existing -> {
                    throw new BusinessException(IamErrorCode.ROLE_ASSIGNMENT_DUPLICATED);
                });

        RoleAssignment assignment =
                RoleAssignment.create(request.userAccountId(), request.roleId());
        roleAssignmentRepository.save(assignment);
        return assignment.getId();
    }

    /** 부여를 회수(소프트 삭제)한다. */
    public void revoke(String assignmentId) {
        RoleAssignment assignment = roleAssignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new BusinessException(IamErrorCode.ROLE_ASSIGNMENT_NOT_FOUND));

        if (assignment.isActive()) {
            Role role = roleRepository.findById(assignment.getRoleId()).orElse(null);
            if (role != null && role.isActive() && role.hasPermission(Permission.ROLE_MANAGE)
                    && roleManagerCounter.count(
                            RoleManagerCounter.Options.excludingAssignment(assignmentId)) == 0) {
                throw new BusinessException(IamErrorCode.ROLE_LOCKOUT_FORBIDDEN);
            }
        }

        assignment.revoke();
        roleAssignmentRepository.save(assignment);
    }
}
