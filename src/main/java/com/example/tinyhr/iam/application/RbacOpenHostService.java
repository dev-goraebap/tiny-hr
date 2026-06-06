package com.example.tinyhr.iam.application;

import com.example.tinyhr.iam.domain.rbac.Permission;
import com.example.tinyhr.iam.domain.role.Role;
import com.example.tinyhr.iam.domain.role.RoleRepository;
import com.example.tinyhr.iam.domain.roleassignment.RoleAssignment;
import com.example.tinyhr.iam.domain.roleassignment.RoleAssignmentRepository;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * RBAC 컨텍스트가 외부에 공개하는 Open Host Service.
 *
 * <p>모든 컨텍스트의 유즈케이스·가드가 이 서비스 한 토큰으로 권한을 확인한다. 사원의 <b>활성</b>
 * 부여를 모두 모아 각 <b>활성</b> 역할의 권한을 합집합(Union)하여 판정한다.
 */
@Service
@Transactional(readOnly = true)
public class RbacOpenHostService {

    private final RoleAssignmentRepository roleAssignmentRepository;
    private final RoleRepository roleRepository;

    public RbacOpenHostService(
            RoleAssignmentRepository roleAssignmentRepository, RoleRepository roleRepository) {
        this.roleAssignmentRepository = roleAssignmentRepository;
        this.roleRepository = roleRepository;
    }

    /** 사원이 해당 권한을 실효 보유하는지. 가드 판정용. */
    public boolean has(String userAccountId, Permission permission) {
        return activeRolesOf(userAccountId).stream().anyMatch(role -> role.hasPermission(permission));
    }

    /**
     * 사원의 <b>유효 권한 전체</b>(활성 부여 × 활성 역할의 Union).
     * 프론트 메뉴·버튼 조건부 표시용. 단건 가드 판정에는 {@link #has} 권장.
     */
    public List<Permission> listEffective(String userAccountId) {
        Set<Permission> union = new LinkedHashSet<>();
        for (Role role : activeRolesOf(userAccountId)) {
            union.addAll(role.getPermissions());
        }
        return List.copyOf(union);
    }

    /** 사원의 활성 부여가 가리키는 활성 역할들. */
    private List<Role> activeRolesOf(String userAccountId) {
        List<RoleAssignment> active =
                roleAssignmentRepository.findByUserAccountIdAndRevokedAtIsNull(userAccountId);
        if (active.isEmpty()) {
            return List.of();
        }
        List<String> roleIds = active.stream().map(RoleAssignment::getRoleId).distinct().toList();
        return roleRepository.findAllById(roleIds).stream().filter(Role::isActive).toList();
    }
}
