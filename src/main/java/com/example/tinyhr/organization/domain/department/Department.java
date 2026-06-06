package com.example.tinyhr.organization.domain.department;

import com.example.tinyhr.organization.domain.OrganizationErrorCode;
import com.example.tinyhr.shared.kernel.BusinessException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 부서 트리 애그리거트 루트 (부서 &gt; 팀 2뎁스 제한). 아카이브 = 소프트 삭제.
 *
 * 부서장(lead) 관련 상태/행위는 employee 애그리거트 이식 후 추가한다.
 */
@Entity
@Table(name = "department")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Department {

    @Id
    @Column(name = "department_id", length = 36)
    private String id;

    @Column(nullable = false)
    private String name;

    /** 상위 부서 ID. null = 최상위(부서). */
    @Column(name = "parent_id", length = 36)
    private String parentId;

    /** 0 = 부서, 1 = 팀. 2 이상 금지. */
    @Column(nullable = false)
    private int depth;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(name = "responsibilities")
    private String responsibilities;

    @Column(name = "archived_at")
    private Instant archivedAt;

    /** 새 부서(또는 하위 팀) 생성. 깊이·상위 활성 불변식을 강제한다. */
    public static Department create(Department parent, String name) {
        int depth = parent == null ? 0 : parent.depth + 1;
        if (depth > 1) {
            throw new BusinessException(OrganizationErrorCode.DEPARTMENT_DEPTH_EXCEEDED);
        }
        if (parent != null && !parent.active) {
            throw new BusinessException(OrganizationErrorCode.DEPARTMENT_PARENT_INACTIVE);
        }
        Department department = new Department();
        department.id = UUID.randomUUID().toString();
        department.name = name.trim();
        department.parentId = parent == null ? null : parent.id;
        department.depth = depth;
        department.active = true;
        department.displayOrder = 0;
        department.responsibilities = null;
        department.archivedAt = null;
        return department;
    }

    public void rename(String newName) {
        ensureActive();
        this.name = newName.trim();
    }

    public void updateResponsibilities(String value) {
        ensureActive();
        this.responsibilities = sanitize(value);
    }

    public void changeOrder(int newOrder) {
        this.displayOrder = newOrder;
    }

    public void archive() {
        this.active = false;
        this.archivedAt = Instant.now();
    }

    private void ensureActive() {
        if (!active) {
            throw new BusinessException(OrganizationErrorCode.DEPARTMENT_ARCHIVED);
        }
    }

    private static String sanitize(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
