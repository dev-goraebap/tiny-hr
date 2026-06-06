package com.example.tinyhr.organization.domain.rank;

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
 * 직급(전사 단일 트랙) 애그리거트 루트.
 *
 * 내부 상태는 private, 변경은 도메인 메서드로만 한다(setter 없음).
 * 소프트 삭제는 {@code archivedAt} + {@code active} 로 표현한다.
 */
@Entity
@Table(name = "ranks") // rank 는 SQL 예약어(RANK())라 복수형 테이블명으로 회피
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Rank {

    @Id
    @Column(name = "rank_id", length = 36)
    private String id;

    @Column(nullable = false)
    private String name;

    /** 노출 순서. SQL 예약어(order) 회피 위해 displayOrder. */
    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(name = "career_criteria")
    private String careerCriteria;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "archived_at")
    private Instant archivedAt;

    /** 새 직급 생성. 식별자는 도메인이 발급한다. */
    public static Rank create(String name, int displayOrder, String careerCriteria) {
        Rank rank = new Rank();
        rank.id = UUID.randomUUID().toString();
        rank.name = name.trim();
        rank.displayOrder = displayOrder;
        rank.careerCriteria = sanitize(careerCriteria);
        rank.active = true;
        rank.archivedAt = null;
        return rank;
    }

    public void rename(String newName) {
        this.name = newName.trim();
    }

    public void updateCareerCriteria(String value) {
        this.careerCriteria = sanitize(value);
    }

    public void changeOrder(int newOrder) {
        this.displayOrder = newOrder;
    }

    /** 아카이브(소프트 삭제). 이후 목록·조회에서 비활성으로 분류된다. */
    public void archive() {
        this.active = false;
        this.archivedAt = Instant.now();
    }

    private static String sanitize(String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
