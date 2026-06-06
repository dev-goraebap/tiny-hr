package com.example.tinyhr.organization.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.example.tinyhr.organization.application.dto.CreatePositionRequest;
import com.example.tinyhr.organization.application.dto.ReorderPositionsRequest;
import com.example.tinyhr.organization.application.dto.UpdatePositionRequest;
import com.example.tinyhr.organization.domain.OrganizationErrorCode;
import com.example.tinyhr.organization.domain.position.Position;
import com.example.tinyhr.organization.domain.position.PositionRepository;
import com.example.tinyhr.shared.kernel.BusinessException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PositionServiceTest {

    @Mock
    PositionRepository positionRepository;

    @InjectMocks
    PositionService positionService;

    private static void assertBusiness(ThrowingCallable callable, OrganizationErrorCode expected) {
        assertThatThrownBy(callable)
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(expected);
    }

    @Test
    @DisplayName("새 직위를 등록한다")
    void register() {
        // given
        given(positionRepository.existsByName("팀장")).willReturn(false);
        given(positionRepository.existsByDisplayOrder(1)).willReturn(false);

        // when
        String id = positionService.create(new CreatePositionRequest("팀장", 1, null));

        // then
        assertThat(id).isNotBlank();
        then(positionRepository).should().save(any(Position.class));
    }

    @Test
    @DisplayName("이미 같은 이름의 직위가 있으면 등록할 수 없다")
    void rejectDuplicateName() {
        // given
        given(positionRepository.existsByName("팀장")).willReturn(true);

        // when & then
        assertBusiness(() -> positionService.create(new CreatePositionRequest("팀장", 1, null)),
                OrganizationErrorCode.POSITION_NAME_DUPLICATED);
        then(positionRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("이미 같은 정렬 순서의 직위가 있으면 등록할 수 없다")
    void rejectDuplicateOrder() {
        // given
        given(positionRepository.existsByName("팀장")).willReturn(false);
        given(positionRepository.existsByDisplayOrder(1)).willReturn(true);

        // when & then
        assertBusiness(() -> positionService.create(new CreatePositionRequest("팀장", 1, null)),
                OrganizationErrorCode.POSITION_ORDER_DUPLICATED);
    }

    @Test
    @DisplayName("없는 직위는 수정할 수 없다")
    void rejectUpdateWhenNotFound() {
        // given
        given(positionRepository.findById("none")).willReturn(Optional.empty());

        // when & then
        assertBusiness(() -> positionService.update("none", new UpdatePositionRequest("이름", null)),
                OrganizationErrorCode.POSITION_NOT_FOUND);
    }

    @Test
    @DisplayName("다른 직위와 이름이 겹치면 수정할 수 없다")
    void rejectUpdateWithDuplicateName() {
        // given
        Position position = Position.create("사원", 0, null);
        given(positionRepository.findById(position.getId())).willReturn(Optional.of(position));
        given(positionRepository.existsByNameAndIdNot("팀장", position.getId())).willReturn(true);

        // when & then
        assertBusiness(
                () -> positionService.update(position.getId(), new UpdatePositionRequest("팀장", null)),
                OrganizationErrorCode.POSITION_NAME_DUPLICATED);
    }

    @Test
    @DisplayName("같은 직위를 두 번 지정해 재정렬할 수 없다")
    void rejectReorderWithDuplicateId() {
        // when & then
        assertBusiness(() -> positionService.reorder(new ReorderPositionsRequest(List.of("a", "a"))),
                OrganizationErrorCode.POSITION_REORDER_DUPLICATE_ID);
    }

    @Test
    @DisplayName("활성 직위 전체를 지정하지 않으면 재정렬할 수 없다")
    void rejectIncompleteReorder() {
        // given
        Position active = Position.create("사원", 0, null);
        given(positionRepository.findByActiveTrue()).willReturn(List.of(active));

        // when & then
        assertBusiness(() -> positionService.reorder(new ReorderPositionsRequest(List.of("ghost"))),
                OrganizationErrorCode.POSITION_REORDER_INCOMPLETE);
    }

    @Test
    @DisplayName("지정한 순서대로 직위 노출 순서가 매겨진다")
    void reorder() {
        // given
        Position a = Position.create("a", 5, null);
        Position b = Position.create("b", 9, null);
        given(positionRepository.findByActiveTrue()).willReturn(new ArrayList<>(List.of(a, b)));

        // when (b 를 먼저)
        positionService.reorder(new ReorderPositionsRequest(List.of(b.getId(), a.getId())));

        // then
        assertThat(b.getDisplayOrder()).isEqualTo(1);
        assertThat(a.getDisplayOrder()).isEqualTo(2);
        then(positionRepository).should().saveAll(any());
    }

    @Test
    @DisplayName("없는 직위는 아카이브할 수 없다")
    void rejectArchiveWhenNotFound() {
        // given
        given(positionRepository.findById("none")).willReturn(Optional.empty());

        // when & then
        assertBusiness(() -> positionService.archive("none"), OrganizationErrorCode.POSITION_NOT_FOUND);
    }

    @Test
    @DisplayName("직위를 아카이브한다")
    void archive() {
        // given
        Position position = Position.create("사원", 0, null);
        given(positionRepository.findById(position.getId())).willReturn(Optional.of(position));

        // when
        positionService.archive(position.getId());

        // then
        assertThat(position.isActive()).isFalse();
        then(positionRepository).should().save(position);
    }
}
