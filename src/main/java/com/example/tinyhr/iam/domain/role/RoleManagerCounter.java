package com.example.tinyhr.iam.domain.role;

import java.util.List;

/**
 * 자기 잠금(self-lockout) 방어용 — 변경을 적용했다고 가정했을 때, 활성 {@code ROLE_MANAGE}
 * 권한을 실효(effective)로 보유한 사원 수를 미리 센다.
 *
 * <p>effective 보유자 = ({@code role.active} ∧ {@code role.permissions ∋ ROLE_MANAGE}
 * ∧ {@code roleAssignment.active}) 인 사원. 결과가 0 이면 변경을 거부해야 한다
 * ({@code ROLE_LOCKOUT_FORBIDDEN}).
 *
 * <p>운영 빈도가 낮아 단순 카운트 쿼리 시뮬레이션(best-effort)으로 충분하다.
 */
public interface RoleManagerCounter {

    long count(Options options);

    /**
     * @param excludeAssignmentIds 회수된다고 가정할 부여 ID
     * @param excludeRoleIds 아카이브된다고 가정할 역할 ID
     * @param rolesLosingManagePerm ROLE_MANAGE 가 빠진다고 가정할 역할 ID
     */
    record Options(
            List<String> excludeAssignmentIds,
            List<String> excludeRoleIds,
            List<String> rolesLosingManagePerm) {

        public static Options none() {
            return new Options(List.of(), List.of(), List.of());
        }

        /** 이 부여가 회수된다고 가정. */
        public static Options excludingAssignment(String assignmentId) {
            return new Options(List.of(assignmentId), List.of(), List.of());
        }

        /** 이 역할이 아카이브된다고 가정. */
        public static Options excludingRole(String roleId) {
            return new Options(List.of(), List.of(roleId), List.of());
        }

        /** 이 역할에서 ROLE_MANAGE 가 빠진다고 가정. */
        public static Options roleLosingManage(String roleId) {
            return new Options(List.of(), List.of(), List.of(roleId));
        }
    }
}
