package com.example.tinyhr.organization.domain.rank;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RankTest {

    @Test
    @DisplayName("새 직급은 입력을 정규화해 활성 상태로 만들어진다")
    void create() {
        // when
        Rank rank = Rank.create("  팀장  ", 1, "  3년차  ");

        // then
        assertThat(rank.getId()).isNotBlank();
        assertThat(rank.getName()).isEqualTo("팀장");
        assertThat(rank.getDisplayOrder()).isEqualTo(1);
        assertThat(rank.getCareerCriteria()).isEqualTo("3년차");
        assertThat(rank.isActive()).isTrue();
        assertThat(rank.getArchivedAt()).isNull();
    }

    @Test
    @DisplayName("승진 기준이 비어 있으면 값이 없는 것으로 본다")
    void normalizesBlankCriteria() {
        // when
        Rank rank = Rank.create("사원", 0, "   ");

        // then
        assertThat(rank.getCareerCriteria()).isNull();
    }

    @Test
    @DisplayName("직급 이름을 바꿀 수 있다")
    void rename() {
        // given
        Rank rank = Rank.create("사원", 0, null);

        // when
        rank.rename("  수석  ");

        // then
        assertThat(rank.getName()).isEqualTo("수석");
    }

    @Test
    @DisplayName("직급을 아카이브하면 비활성이 된다")
    void archive() {
        // given
        Rank rank = Rank.create("사원", 0, null);

        // when
        rank.archive();

        // then
        assertThat(rank.isActive()).isFalse();
        assertThat(rank.getArchivedAt()).isNotNull();
    }

    @Test
    @DisplayName("직급 노출 순서를 바꿀 수 있다")
    void changeOrder() {
        // given
        Rank rank = Rank.create("사원", 0, null);

        // when
        rank.changeOrder(5);

        // then
        assertThat(rank.getDisplayOrder()).isEqualTo(5);
    }
}
