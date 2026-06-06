package com.example.tinyhr.organization.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

    /** BusinessException 이 던져지고 그 ErrorCode 가 기대값인지 검증. */
    private static void assertBusiness(ThrowingCallable callable, OrganizationErrorCode expected) {
        assertThatThrownBy(callable)
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(expected);
    }

    @Test
    @DisplayName("직위를 등록하면 식별자를 반환하고 저장한다")
    void 등록_성공() {
        // given
        when(positionRepository.existsByName("팀장")).thenReturn(false);
        when(positionRepository.existsByDisplayOrder(1)).thenReturn(false);

        // when
        String id = positionService.create(new CreatePositionRequest("팀장", 1, null));

        // then
        assertThat(id).isNotBlank();
        verify(positionRepository).save(any(Position.class));
    }

    @Test
    @DisplayName("직위명이 중복이면 POSITION_NAME_DUPLICATED")
    void 등록_이름중복() {
        // given
        when(positionRepository.existsByName("팀장")).thenReturn(true);

        // when & then
        assertBusiness(() -> positionService.create(new CreatePositionRequest("팀장", 1, null)),
                OrganizationErrorCode.POSITION_NAME_DUPLICATED);
        verify(positionRepository, never()).save(any());
    }

    @Test
    @DisplayName("정렬 순서가 중복이면 POSITION_ORDER_DUPLICATED")
    void 등록_순서중복() {
        // given
        when(positionRepository.existsByName("팀장")).thenReturn(false);
        when(positionRepository.existsByDisplayOrder(1)).thenReturn(true);

        // when & then
        assertBusiness(() -> positionService.create(new CreatePositionRequest("팀장", 1, null)),
                OrganizationErrorCode.POSITION_ORDER_DUPLICATED);
    }

    @Test
    @DisplayName("없는 직위 수정은 POSITION_NOT_FOUND")
    void 수정_없음() {
        // given
        when(positionRepository.findById("none")).thenReturn(Optional.empty());

        // when & then
        assertBusiness(() -> positionService.update("none", new UpdatePositionRequest("이름", null)),
                OrganizationErrorCode.POSITION_NOT_FOUND);
    }

    @Test
    @DisplayName("다른 직위와 이름이 겹치면 POSITION_NAME_DUPLICATED")
    void 수정_이름중복() {
        // given
        Position position = Position.create("사원", 0, null);
        when(positionRepository.findById(position.getId())).thenReturn(Optional.of(position));
        when(positionRepository.existsByNameAndIdNot("팀장", position.getId())).thenReturn(true);

        // when & then
        assertBusiness(
                () -> positionService.update(position.getId(), new UpdatePositionRequest("팀장", null)),
                OrganizationErrorCode.POSITION_NAME_DUPLICATED);
    }

    @Test
    @DisplayName("재정렬 입력에 같은 ID 가 중복이면 POSITION_REORDER_DUPLICATE_ID")
    void 재정렬_중복ID() {
        assertBusiness(() -> positionService.reorder(new ReorderPositionsRequest(List.of("a", "a"))),
                OrganizationErrorCode.POSITION_REORDER_DUPLICATE_ID);
    }

    @Test
    @DisplayName("재정렬 대상이 활성 직위 전체가 아니면 POSITION_REORDER_INCOMPLETE")
    void 재정렬_불완전() {
        // given
        Position active = Position.create("사원", 0, null);
        when(positionRepository.findByActiveTrue()).thenReturn(List.of(active));

        // when & then (활성에 없는 'ghost' 지정)
        assertBusiness(() -> positionService.reorder(new ReorderPositionsRequest(List.of("ghost"))),
                OrganizationErrorCode.POSITION_REORDER_INCOMPLETE);
    }

    @Test
    @DisplayName("재정렬하면 입력 순서대로 displayOrder 가 1부터 매겨진다")
    void 재정렬_성공() {
        // given
        Position a = Position.create("a", 5, null);
        Position b = Position.create("b", 9, null);
        when(positionRepository.findByActiveTrue()).thenReturn(new ArrayList<>(List.of(a, b)));

        // when (b 를 먼저)
        positionService.reorder(new ReorderPositionsRequest(List.of(b.getId(), a.getId())));

        // then
        assertThat(b.getDisplayOrder()).isEqualTo(1);
        assertThat(a.getDisplayOrder()).isEqualTo(2);
        verify(positionRepository).saveAll(any());
    }

    @Test
    @DisplayName("없는 직위 아카이브는 POSITION_NOT_FOUND")
    void 아카이브_없음() {
        when(positionRepository.findById("none")).thenReturn(Optional.empty());
        assertBusiness(() -> positionService.archive("none"), OrganizationErrorCode.POSITION_NOT_FOUND);
    }

    @Test
    @DisplayName("아카이브하면 비활성 처리 후 저장한다")
    void 아카이브_성공() {
        // given
        Position position = Position.create("사원", 0, null);
        when(positionRepository.findById(position.getId())).thenReturn(Optional.of(position));

        // when
        positionService.archive(position.getId());

        // then
        assertThat(position.isActive()).isFalse();
        verify(positionRepository).save(position);
    }
}
