package com.example.tinyhr.organization.adapter.web;

import com.example.tinyhr.organization.adapter.mapper.PositionListItem;
import com.example.tinyhr.organization.adapter.mapper.PositionQueryMapper;
import com.example.tinyhr.organization.application.PositionService;
import com.example.tinyhr.organization.application.dto.CreatePositionRequest;
import com.example.tinyhr.organization.application.dto.ReorderPositionsRequest;
import com.example.tinyhr.organization.application.dto.UpdatePositionRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** 직위(전사 단일 트랙) 관리·조회 HTTP 진입점. */
@RestController
@RequestMapping("/admin/positions")
public class PositionController {

    private final PositionService positionService;
    private final PositionQueryMapper positionQueryMapper;

    public PositionController(PositionService positionService, PositionQueryMapper positionQueryMapper) {
        this.positionService = positionService;
        this.positionQueryMapper = positionQueryMapper;
    }

    @GetMapping
    public Map<String, List<PositionListItem>> list() {
        return Map.of("items", positionQueryMapper.listForAdmin());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, String> create(@Valid @RequestBody CreatePositionRequest request) {
        return Map.of("positionId", positionService.create(request));
    }

    @PostMapping("/reorder")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reorder(@Valid @RequestBody ReorderPositionsRequest request) {
        positionService.reorder(request);
    }

    @PatchMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void update(@PathVariable String id, @Valid @RequestBody UpdatePositionRequest request) {
        positionService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@PathVariable String id) {
        positionService.archive(id);
    }

    @PostMapping("/{id}/archive")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void archive(@PathVariable String id) {
        positionService.archive(id);
    }
}
