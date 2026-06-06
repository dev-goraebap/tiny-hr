package com.example.tinyhr.organization.domain.position;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PositionTest {

    @Test
    @DisplayName("새 직위는 입력을 정규화해 활성 상태로 만들어진다")
    void create() {
        // when
        Position position = Position.create("  팀장  ", 1, "  3년차  ");

        // then
        assertThat(position.getId()).isNotBlank();
        assertThat(position.getName()).isEqualTo("팀장");
        assertThat(position.getDisplayOrder()).isEqualTo(1);
        assertThat(position.getCareerCriteria()).isEqualTo("3년차");
        assertThat(position.isActive()).isTrue();
        assertThat(position.getArchivedAt()).isNull();
    }

    @Test
    @DisplayName("승진 기준이 비어 있으면 값이 없는 것으로 본다")
    void normalizesBlankCriteria() {
        // when
        Position position = Position.create("사원", 0, "   ");

        // then
        assertThat(position.getCareerCriteria()).isNull();
    }

    @Test
    @DisplayName("직위 이름을 바꿀 수 있다")
    void rename() {
        // given
        Position position = Position.create("사원", 0, null);

        // when
        position.rename("  수석  ");

        // then
        assertThat(position.getName()).isEqualTo("수석");
    }

    @Test
    @DisplayName("직위를 아카이브하면 비활성이 된다")
    void archive() {
        // given
        Position position = Position.create("사원", 0, null);

        // when
        position.archive();

        // then
        assertThat(position.isActive()).isFalse();
        assertThat(position.getArchivedAt()).isNotNull();
    }

    @Test
    @DisplayName("직위 노출 순서를 바꿀 수 있다")
    void changeOrder() {
        // given
        Position position = Position.create("사원", 0, null);

        // when
        position.changeOrder(5);

        // then
        assertThat(position.getDisplayOrder()).isEqualTo(5);
    }
}
