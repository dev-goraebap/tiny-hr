package com.example.tinyhr.iam.application;

import com.example.tinyhr.iam.application.dto.CreateRoleRequest;
import com.example.tinyhr.iam.application.dto.UpdateRoleRequest;
import com.example.tinyhr.iam.domain.IamErrorCode;
import com.example.tinyhr.iam.domain.rbac.Permission;
import com.example.tinyhr.iam.domain.role.Role;
import com.example.tinyhr.iam.domain.role.RoleManagerCounter;
import com.example.tinyhr.iam.domain.role.RoleRepository;
import com.example.tinyhr.iam.domain.roleassignment.RoleAssignment;
import com.example.tinyhr.iam.domain.roleassignment.RoleAssignmentRepository;
import com.example.tinyhr.shared.kernel.BusinessException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 역할 생성·수정·아카이브·재활성화 오케스트레이션.
 *
 * <p>자기 잠금(self-lockout) 방어 — 활성 {@code ROLE_MANAGE} 보유자가 0 이 되는 변경은 거부한다.
 *
 * @actor 관리자
 */
@Service
@Transactional
public class RoleService {

    private final RoleRepository roleRepository;
    private final RoleAssignmentRepository roleAssignmentRepository;
    private final RoleManagerCounter roleManagerCounter;

    public RoleService(
            RoleRepository roleRepository,
            RoleAssignmentRepository roleAssignmentRepository,
            RoleManagerCounter roleManagerCounter) {
        this.roleRepository = roleRepository;
        this.roleAssignmentRepository = roleAssignmentRepository;
        this.roleManagerCounter = roleManagerCounter;
    }

    /** 신규 역할 생성. 이름은 대소문자 무시 유일. */
    public String create(CreateRoleRequest request) {
        roleRepository.findByNameIgnoreCase(request.name().trim())
                .ifPresent(existing -> {
                    throw new BusinessException(IamErrorCode.ROLE_NAME_DUPLICATED);
                });
        Role role = Role.create(request.name(), request.description(), request.permissions());
        roleRepository.save(role);
        return role.getId();
    }

    /** 역할의 이름·설명·권한 부분 갱신. null 필드는 변경하지 않는다. */
    public void update(String roleId, UpdateRoleRequest request) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new BusinessException(IamErrorCode.ROLE_NOT_FOUND));

        // 1) 권한 변경에서 ROLE_MANAGE 가 빠지는 경우만 사전 lockout 검증.
        if (request.permissions() != null) {
            boolean hadManage = role.hasPermission(Permission.ROLE_MANAGE);
            boolean willHaveManage = request.permissions().contains(Permission.ROLE_MANAGE);
            if (hadManage && !willHaveManage && role.isActive()
                    && roleManagerCounter.count(RoleManagerCounter.Options.roleLosingManage(roleId)) == 0) {
                throw new BusinessException(IamErrorCode.ROLE_LOCKOUT_FORBIDDEN);
            }
        }

        // 2) 이름 변경 시 중복 검사 — 자기 자신 제외.
        if (request.name() != null) {
            roleRepository.findByNameIgnoreCase(request.name().trim())
                    .filter(dup -> !dup.getId().equals(roleId))
                    .ifPresent(dup -> {
                        throw new BusinessException(IamErrorCode.ROLE_NAME_DUPLICATED);
                    });
            role.rename(request.name());
        }
        if (request.description() != null) {
            role.updateDescription(request.description());
        }
        if (request.permissions() != null) {
            role.changePermissions(request.permissions());
        }
        roleRepository.save(role);
    }

    /** 역할 아카이브(비활성) + 활성 부여 cascade revoke. */
    public void archive(String roleId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new BusinessException(IamErrorCode.ROLE_NOT_FOUND));

        if (role.isActive() && role.hasPermission(Permission.ROLE_MANAGE)
                && roleManagerCounter.count(RoleManagerCounter.Options.excludingRole(roleId)) == 0) {
            throw new BusinessException(IamErrorCode.ROLE_LOCKOUT_FORBIDDEN);
        }

        role.archive();
        roleRepository.save(role);

        List<RoleAssignment> active = roleAssignmentRepository.findByRoleIdAndRevokedAtIsNull(roleId);
        active.forEach(RoleAssignment::revoke);
        roleAssignmentRepository.saveAll(active);
    }

    /** 아카이브된 역할을 재활성화한다. */
    public void reactivate(String roleId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new BusinessException(IamErrorCode.ROLE_NOT_FOUND));
        role.reactivate();
        roleRepository.save(role);
    }
}
