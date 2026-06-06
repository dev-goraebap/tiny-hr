package com.example.tinyhr.iam.adapter.persistence;

import com.example.tinyhr.iam.domain.rbac.Permission;
import com.example.tinyhr.iam.domain.role.RoleManagerCounter;
import com.example.tinyhr.iam.domain.roleassignment.RoleAssignmentRepository;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * {@link RoleManagerCounter} 의 운영 구현체.
 *
 * <p>도메인 포트가 표현한 "변경을 가정한 활성 ROLE_MANAGE 보유자 수"를 {@link RoleAssignmentRepository}
 * 의 카운트 쿼리로 계산한다. JPQL {@code not in} 은 빈 컬렉션을 허용하지 않으므로, 어디에도 매칭되지
 * 않는 센티넬 값을 항상 한 개 끼워 컬렉션이 비지 않게 한다.
 */
@Component
public class RoleManagerCounterAdapter implements RoleManagerCounter {

    /** 실 ID(UUID)와 절대 겹치지 않는 센티넬 — not-in 컬렉션을 비우지 않기 위함. */
    private static final String NONE = "-";

    private final RoleAssignmentRepository roleAssignmentRepository;

    public RoleManagerCounterAdapter(RoleAssignmentRepository roleAssignmentRepository) {
        this.roleAssignmentRepository = roleAssignmentRepository;
    }

    @Override
    public long count(Options options) {
        List<String> excludeAssignmentIds = withSentinel(options.excludeAssignmentIds());
        List<String> excludeRoleIds = withSentinel(
                concat(options.excludeRoleIds(), options.rolesLosingManagePerm()));
        return roleAssignmentRepository.countEffectiveHolders(
                Permission.ROLE_MANAGE, excludeAssignmentIds, excludeRoleIds);
    }

    private static List<String> concat(List<String> a, List<String> b) {
        List<String> out = new ArrayList<>();
        if (a != null) {
            out.addAll(a);
        }
        if (b != null) {
            out.addAll(b);
        }
        return out;
    }

    private static List<String> withSentinel(List<String> ids) {
        List<String> out = new ArrayList<>(ids);
        out.add(NONE);
        return out;
    }
}
