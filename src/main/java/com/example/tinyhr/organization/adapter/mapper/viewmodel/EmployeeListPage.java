package com.example.tinyhr.organization.adapter.mapper.viewmodel;

import java.util.List;

/** 사원 목록 페이지(조회 결과 + 전체 건수). */
public record EmployeeListPage(List<EmployeeListItem> items, long total) {}
