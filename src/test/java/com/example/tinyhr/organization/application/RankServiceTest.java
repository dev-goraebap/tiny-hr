package com.example.tinyhr.organization.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.example.tinyhr.organization.application.dto.CreateRankRequest;
import com.example.tinyhr.organization.application.dto.ReorderRanksRequest;
import com.example.tinyhr.organization.application.dto.UpdateRankRequest;
import com.example.tinyhr.organization.domain.OrganizationErrorCode;
import com.example.tinyhr.organization.domain.rank.Rank;
import com.example.tinyhr.organization.domain.rank.RankRepository;
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
class RankServiceTest {

    @Mock
    RankRepository rankRepository;

    @InjectMocks
    RankService rankService;

    private static void assertBusiness(ThrowingCallable callable, OrganizationErrorCode expected) {
        assertThatThrownBy(callable)
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(expected);
    }

    @Test
    @DisplayName("새 직급을 등록한다")
    void register() {
        // given
        given(rankRepository.existsByName("팀장")).willReturn(false);
        given(rankRepository.existsByDisplayOrder(1)).willReturn(false);

        // when
        String id = rankService.create(new CreateRankRequest("팀장", 1, null));

        // then
        assertThat(id).isNotBlank();
        then(rankRepository).should().save(any(Rank.class));
    }

    @Test
    @DisplayName("이미 같은 이름의 직급이 있으면 등록할 수 없다")
    void rejectDuplicateName() {
        // given
        given(rankRepository.existsByName("팀장")).willReturn(true);

        // when & then
        assertBusiness(() -> rankService.create(new CreateRankRequest("팀장", 1, null)),
                OrganizationErrorCode.RANK_NAME_DUPLICATED);
        then(rankRepository).should(never()).save(any());
    }

    @Test
    @DisplayName("이미 같은 정렬 순서의 직급이 있으면 등록할 수 없다")
    void rejectDuplicateOrder() {
        // given
        given(rankRepository.existsByName("팀장")).willReturn(false);
        given(rankRepository.existsByDisplayOrder(1)).willReturn(true);

        // when & then
        assertBusiness(() -> rankService.create(new CreateRankRequest("팀장", 1, null)),
                OrganizationErrorCode.RANK_ORDER_DUPLICATED);
    }

    @Test
    @DisplayName("없는 직급은 수정할 수 없다")
    void rejectUpdateWhenNotFound() {
        // given
        given(rankRepository.findById("none")).willReturn(Optional.empty());

        // when & then
        assertBusiness(() -> rankService.update("none", new UpdateRankRequest("이름", null)),
                OrganizationErrorCode.RANK_NOT_FOUND);
    }

    @Test
    @DisplayName("다른 직급과 이름이 겹치면 수정할 수 없다")
    void rejectUpdateWithDuplicateName() {
        // given
        Rank rank = Rank.create("사원", 0, null);
        given(rankRepository.findById(rank.getId())).willReturn(Optional.of(rank));
        given(rankRepository.existsByNameAndIdNot("팀장", rank.getId())).willReturn(true);

        // when & then
        assertBusiness(
                () -> rankService.update(rank.getId(), new UpdateRankRequest("팀장", null)),
                OrganizationErrorCode.RANK_NAME_DUPLICATED);
    }

    @Test
    @DisplayName("같은 직급을 두 번 지정해 재정렬할 수 없다")
    void rejectReorderWithDuplicateId() {
        // when & then
        assertBusiness(() -> rankService.reorder(new ReorderRanksRequest(List.of("a", "a"))),
                OrganizationErrorCode.RANK_REORDER_DUPLICATE_ID);
    }

    @Test
    @DisplayName("활성 직급 전체를 지정하지 않으면 재정렬할 수 없다")
    void rejectIncompleteReorder() {
        // given
        Rank active = Rank.create("사원", 0, null);
        given(rankRepository.findByActiveTrue()).willReturn(List.of(active));

        // when & then
        assertBusiness(() -> rankService.reorder(new ReorderRanksRequest(List.of("ghost"))),
                OrganizationErrorCode.RANK_REORDER_INCOMPLETE);
    }

    @Test
    @DisplayName("지정한 순서대로 직급 노출 순서가 매겨진다")
    void reorder() {
        // given
        Rank a = Rank.create("a", 5, null);
        Rank b = Rank.create("b", 9, null);
        given(rankRepository.findByActiveTrue()).willReturn(new ArrayList<>(List.of(a, b)));

        // when (b 를 먼저)
        rankService.reorder(new ReorderRanksRequest(List.of(b.getId(), a.getId())));

        // then
        assertThat(b.getDisplayOrder()).isEqualTo(1);
        assertThat(a.getDisplayOrder()).isEqualTo(2);
        then(rankRepository).should().saveAll(any());
    }

    @Test
    @DisplayName("없는 직급은 아카이브할 수 없다")
    void rejectArchiveWhenNotFound() {
        // given
        given(rankRepository.findById("none")).willReturn(Optional.empty());

        // when & then
        assertBusiness(() -> rankService.archive("none"), OrganizationErrorCode.RANK_NOT_FOUND);
    }

    @Test
    @DisplayName("직급을 아카이브한다")
    void archive() {
        // given
        Rank rank = Rank.create("사원", 0, null);
        given(rankRepository.findById(rank.getId())).willReturn(Optional.of(rank));

        // when
        rankService.archive(rank.getId());

        // then
        assertThat(rank.isActive()).isFalse();
        then(rankRepository).should().save(rank);
    }
}
