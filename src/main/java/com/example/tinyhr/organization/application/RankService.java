package com.example.tinyhr.organization.application;

import com.example.tinyhr.organization.application.dto.CreateRankRequest;
import com.example.tinyhr.organization.application.dto.ReorderRanksRequest;
import com.example.tinyhr.organization.application.dto.UpdateRankRequest;
import com.example.tinyhr.organization.domain.OrganizationErrorCode;
import com.example.tinyhr.organization.domain.rank.Rank;
import com.example.tinyhr.organization.domain.rank.RankRepository;
import com.example.tinyhr.shared.kernel.BusinessException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 직급(전사 단일 트랙)을 등록·수정·정렬·아카이브한다.
 *
 * @actor 관리자
 */
@Service
@Transactional
public class RankService {

    private final RankRepository rankRepository;

    public RankService(RankRepository rankRepository) {
        this.rankRepository = rankRepository;
    }

    /** 새 직급 등록. 직급명과 정렬 순서는 서로 겹칠 수 없다. */
    public String create(CreateRankRequest request) {
        String name = request.name().trim();
        if (rankRepository.existsByName(name)) {
            throw new BusinessException(OrganizationErrorCode.RANK_NAME_DUPLICATED);
        }
        if (rankRepository.existsByDisplayOrder(request.displayOrder())) {
            throw new BusinessException(OrganizationErrorCode.RANK_ORDER_DUPLICATED);
        }
        Rank rank = Rank.create(name, request.displayOrder(), request.careerCriteria());
        rankRepository.save(rank);
        return rank.getId();
    }

    /** 직급 정보 수정(이름·승진 기준). null 필드는 변경하지 않는다. */
    public void update(String rankId, UpdateRankRequest request) {
        Rank rank = rankRepository.findById(rankId)
                .orElseThrow(() -> new BusinessException(OrganizationErrorCode.RANK_NOT_FOUND));

        if (request.name() != null) {
            if (rankRepository.existsByNameAndIdNot(request.name().trim(), rankId)) {
                throw new BusinessException(OrganizationErrorCode.RANK_NAME_DUPLICATED);
            }
            rank.rename(request.name());
        }
        if (request.careerCriteria() != null) {
            rank.updateCareerCriteria(request.careerCriteria());
        }
        rankRepository.save(rank);
    }

    /** 활성 직급 노출 순서 재정렬. 활성 직급 전체를 빠짐없이 한 번씩 지정해야 한다. */
    public void reorder(ReorderRanksRequest request) {
        List<String> ordered = request.orderedRankIds();
        Set<String> inputIds = new LinkedHashSet<>(ordered);
        if (inputIds.size() != ordered.size()) {
            throw new BusinessException(OrganizationErrorCode.RANK_REORDER_DUPLICATE_ID);
        }

        List<Rank> active = rankRepository.findByActiveTrue();
        Set<String> activeIds = active.stream().map(Rank::getId).collect(Collectors.toSet());
        if (!activeIds.containsAll(ordered) || inputIds.size() != activeIds.size()) {
            throw new BusinessException(OrganizationErrorCode.RANK_REORDER_INCOMPLETE);
        }

        Map<String, Rank> byId = active.stream()
                .collect(Collectors.toMap(Rank::getId, Function.identity()));
        for (int i = 0; i < ordered.size(); i++) {
            byId.get(ordered.get(i)).changeOrder(i + 1);
        }
        rankRepository.saveAll(active);
    }

    /** 직급 아카이브(소프트 삭제). */
    public void archive(String rankId) {
        Rank rank = rankRepository.findById(rankId)
                .orElseThrow(() -> new BusinessException(OrganizationErrorCode.RANK_NOT_FOUND));
        rank.archive();
        rankRepository.save(rank);
    }
}
