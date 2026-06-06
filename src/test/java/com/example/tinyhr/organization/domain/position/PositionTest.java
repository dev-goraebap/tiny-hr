package com.example.tinyhr.organization.domain.position;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PositionTest {

    @Test
    @DisplayName("생성하면 이름이 trim 되고 활성 상태로 시작한다")
    void 생성_정규화_및_활성() {
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
    @DisplayName("빈 승진기준은 null 로 정규화된다")
    void 빈_승진기준_널() {
        Position position = Position.create("사원", 0, "   ");
        assertThat(position.getCareerCriteria()).isNull();
    }

    @Test
    @DisplayName("이름 변경은 trim 된다")
    void 이름변경_trim() {
        Position position = Position.create("사원", 0, null);
        position.rename("  수석  ");
        assertThat(position.getName()).isEqualTo("수석");
    }

    @Test
    @DisplayName("아카이브하면 비활성이 되고 archivedAt 이 기록된다")
    void 아카이브() {
        Position position = Position.create("사원", 0, null);
        position.archive();
        assertThat(position.isActive()).isFalse();
        assertThat(position.getArchivedAt()).isNotNull();
    }

    @Test
    @DisplayName("순서를 변경할 수 있다")
    void 순서변경() {
        Position position = Position.create("사원", 0, null);
        position.changeOrder(5);
        assertThat(position.getDisplayOrder()).isEqualTo(5);
    }
}
