package com.example.tinyhr.organization.application;

import com.example.tinyhr.organization.application.dto.CreatePositionRequest;
import com.example.tinyhr.organization.application.dto.ReorderPositionsRequest;
import com.example.tinyhr.organization.application.dto.UpdatePositionRequest;
import com.example.tinyhr.organization.domain.position.Position;
import com.example.tinyhr.organization.domain.position.PositionReorderDuplicateIdException;
import com.example.tinyhr.organization.domain.position.PositionReorderIncompleteException;
import com.example.tinyhr.organization.domain.position.PositionRepository;
import com.example.tinyhr.shared.kernel.ConflictException;
import com.example.tinyhr.shared.kernel.NotFoundException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 직위(전사 단일 트랙)를 등록·수정·정렬·아카이브한다.
 *
 * @actor 관리자
 */
@Service
@Transactional
public class PositionService {

    private final PositionRepository positionRepository;

    public PositionService(PositionRepository positionRepository) {
        this.positionRepository = positionRepository;
    }

    /** 새 직위 등록. 직위명과 정렬 순서는 서로 겹칠 수 없다. */
    public String create(CreatePositionRequest request) {
        String name = request.name().trim();
        if (positionRepository.existsByName(name)) {
            throw new ConflictException("POSITION_NAME_DUPLICATED");
        }
        if (positionRepository.existsByDisplayOrder(request.displayOrder())) {
            throw new ConflictException("POSITION_ORDER_DUPLICATED");
        }
        Position position = Position.create(name, request.displayOrder(), request.careerCriteria());
        positionRepository.save(position);
        return position.getId();
    }

    /** 직위 정보 수정(이름·승진 기준). null 필드는 변경하지 않는다. */
    public void update(String positionId, UpdatePositionRequest request) {
        Position position = positionRepository.findById(positionId)
                .orElseThrow(() -> new NotFoundException("POSITION_NOT_FOUND"));

        if (request.name() != null) {
            if (positionRepository.existsByNameAndIdNot(request.name().trim(), positionId)) {
                throw new ConflictException("POSITION_NAME_DUPLICATED");
            }
            position.rename(request.name());
        }
        if (request.careerCriteria() != null) {
            position.updateCareerCriteria(request.careerCriteria());
        }
        positionRepository.save(position);
    }

    /** 활성 직위 노출 순서 재정렬. 활성 직위 전체를 빠짐없이 한 번씩 지정해야 한다. */
    public void reorder(ReorderPositionsRequest request) {
        List<String> ordered = request.orderedPositionIds();
        Set<String> inputIds = new LinkedHashSet<>(ordered);
        if (inputIds.size() != ordered.size()) {
            throw new PositionReorderDuplicateIdException();
        }

        List<Position> active = positionRepository.findByActiveTrue();
        Set<String> activeIds = active.stream().map(Position::getId).collect(Collectors.toSet());
        for (String id : ordered) {
            if (!activeIds.contains(id)) {
                throw new PositionReorderIncompleteException("재정렬 대상이 활성 직위가 아닙니다: " + id);
            }
        }
        if (inputIds.size() != activeIds.size()) {
            throw new PositionReorderIncompleteException();
        }

        Map<String, Position> byId = active.stream()
                .collect(Collectors.toMap(Position::getId, Function.identity()));
        for (int i = 0; i < ordered.size(); i++) {
            byId.get(ordered.get(i)).changeOrder(i + 1);
        }
        positionRepository.saveAll(active);
    }

    /** 직위 아카이브(소프트 삭제). */
    public void archive(String positionId) {
        Position position = positionRepository.findById(positionId)
                .orElseThrow(() -> new NotFoundException("POSITION_NOT_FOUND"));
        position.archive();
        positionRepository.save(position);
    }
}
