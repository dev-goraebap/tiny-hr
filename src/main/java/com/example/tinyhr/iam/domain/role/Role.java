package com.example.tinyhr.iam.domain.role;

import com.example.tinyhr.iam.domain.IamErrorCode;
import com.example.tinyhr.iam.domain.rbac.Permission;
import com.example.tinyhr.shared.kernel.BusinessException;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 역할(Role) 애그리거트 루트.
 *
 * <p>불변식
 * <ul>
 *   <li>I1 — 이름은 대소문자 무시 유일(서비스 사전 조회로 방어).</li>
 *   <li>I2 — 시스템 역할({@code system})은 rename·archive 금지.</li>
 *   <li>I3 — 비활성 역할은 속성 편집 금지(재활성화 후 가능).</li>
 * </ul>
 *
 * <p>권한은 {@link Permission} enum 의 부분집합이며 중복 없이 보유한다. 내부 상태는 private,
 * 변경은 도메인 메서드로만 한다(setter 없음).
 */
@Entity
@Table(name = "roles") // role 은 SQL 예약어라 복수형 테이블명으로 회피(ranks 와 동일 컨벤션)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Role {

    @Id
    @Column(name = "role_id", length = 36)
    private String id;

    @Column(nullable = false)
    private String name;

    @Column
    private String description;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "role_permission", joinColumns = @JoinColumn(name = "role_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "permission", nullable = false, length = 40)
    private Set<Permission> permissions = new LinkedHashSet<>();

    @Column(name = "is_system", nullable = false)
    private boolean system;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    /** 새 역할 생성. 식별자는 도메인이 발급한다. */
    public static Role create(String name, String description, Collection<Permission> permissions) {
        return create(name, description, permissions, false);
    }

    /** 시스템(기본 제공) 역할 생성 — rename·archive 가 금지된다. 기본 권한 시드용. */
    public static Role createSystem(
            String name, String description, Collection<Permission> permissions) {
        return create(name, description, permissions, true);
    }

    private static Role create(
            String name, String description, Collection<Permission> permissions, boolean system) {
        Role role = new Role();
        role.id = UUID.randomUUID().toString();
        role.name = normalizeName(name);
        role.description = sanitize(description);
        role.permissions = dedup(permissions);
        role.system = system;
        role.active = true;
        return role;
    }

    public void rename(String newName) {
        if (system) {
            throw new BusinessException(IamErrorCode.ROLE_SYSTEM_RENAME_FORBIDDEN);
        }
        assertActiveForEdit();
        this.name = normalizeName(newName);
    }

    public void updateDescription(String description) {
        assertActiveForEdit();
        this.description = sanitize(description);
    }

    public void changePermissions(Collection<Permission> permissions) {
        assertActiveForEdit();
        this.permissions = dedup(permissions);
    }

    /** 아카이브(비활성). 시스템 역할은 금지. 이미 비활성이면 멱등. */
    public void archive() {
        if (system) {
            throw new BusinessException(IamErrorCode.ROLE_SYSTEM_ARCHIVE_FORBIDDEN);
        }
        if (!active) {
            return;
        }
        this.active = false;
    }

    /** 아카이브된 역할을 재활성화. 이미 활성이면 멱등. */
    public void reactivate() {
        if (active) {
            return;
        }
        this.active = true;
    }

    /** 권한 보유 여부 — 권한 판정 핫패스. */
    public boolean hasPermission(Permission permission) {
        return permissions.contains(permission);
    }

    /** 외부 노출용 불변 뷰(직접 변경 금지). */
    public Set<Permission> getPermissions() {
        return Collections.unmodifiableSet(permissions);
    }

    private void assertActiveForEdit() {
        if (!active) {
            throw new BusinessException(IamErrorCode.ROLE_INACTIVE);
        }
    }

    private static String normalizeName(String raw) {
        if (raw == null) {
            throw new BusinessException(IamErrorCode.ROLE_NAME_INVALID);
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty() || trimmed.length() > 64) {
            throw new BusinessException(IamErrorCode.ROLE_NAME_INVALID);
        }
        return trimmed;
    }

    private static Set<Permission> dedup(Collection<Permission> permissions) {
        Set<Permission> out = new LinkedHashSet<>();
        if (permissions != null) {
            for (Permission p : permissions) {
                if (p != null) {
                    out.add(p);
                }
            }
        }
        return out;
    }

    private static String sanitize(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
