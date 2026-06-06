package com.example.tinyhr.organization.adapter.mapper.viewmodel;

/**
 * 관리자 직급 목록 조회 뷰(읽기 전용).
 *
 * memberCount(재직 인원수)는 employee 애그리거트 이식 후 집계 조인으로 채운다.
 * 현재는 0 으로 내려준다.
 */
public record RankListItem(
        String rankId,
        String name,
        int displayOrder,
        String careerCriteria,
        boolean active,
        long memberCount) {}
