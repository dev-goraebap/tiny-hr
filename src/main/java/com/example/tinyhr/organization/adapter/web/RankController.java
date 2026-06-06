package com.example.tinyhr.organization.adapter.web;

import com.example.tinyhr.organization.adapter.mapper.RankQueryMapper;
import com.example.tinyhr.organization.adapter.mapper.viewmodel.RankListItem;
import com.example.tinyhr.organization.application.RankService;
import com.example.tinyhr.organization.application.dto.CreateRankRequest;
import com.example.tinyhr.organization.application.dto.ReorderRanksRequest;
import com.example.tinyhr.organization.application.dto.UpdateRankRequest;
import com.example.tinyhr.shared.kernel.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
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

/** 직급(전사 단일 트랙) 관리·조회 HTTP 진입점. */
@RestController
@RequestMapping("/admin/ranks")
public class RankController {

    private final RankService rankService;
    private final RankQueryMapper rankQueryMapper;

    public RankController(RankService rankService, RankQueryMapper rankQueryMapper) {
        this.rankService = rankService;
        this.rankQueryMapper = rankQueryMapper;
    }

    @GetMapping
    public ApiResponse<List<RankListItem>> list() {
        return ApiResponse.of(rankQueryMapper.listForAdmin());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void create(@Valid @RequestBody CreateRankRequest request) {
        rankService.create(request);
    }

    @PostMapping("/reorder")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reorder(@Valid @RequestBody ReorderRanksRequest request) {
        rankService.reorder(request);
    }

    @PatchMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void update(@PathVariable String id, @Valid @RequestBody UpdateRankRequest request) {
        rankService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@PathVariable String id) {
        rankService.archive(id);
    }

    @PostMapping("/{id}/archive")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void archive(@PathVariable String id) {
        rankService.archive(id);
    }
}
