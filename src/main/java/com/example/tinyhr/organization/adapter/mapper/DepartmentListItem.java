package com.example.tinyhr.organization.adapter.mapper;

/** 부서 목록 조회 뷰(읽기 전용). 트리 표시는 parentId/depth 로 클라이언트가 구성. */
public record DepartmentListItem(
        String departmentId,
        String name,
        String parentId,
        int depth,
        boolean active,
        int displayOrder,
        String responsibilities) {}
