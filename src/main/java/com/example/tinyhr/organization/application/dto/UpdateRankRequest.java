package com.example.tinyhr.organization.application.dto;

import jakarta.validation.constraints.Size;

/**
 * 직급 수정 요청. 각 필드는 null 이면 "변경 안 함"을 의미한다.
 * (참고 프로젝트 단순화: careerCriteria 를 빈 값으로 비우는 시나리오는 다루지 않음)
 */
public record UpdateRankRequest(
        @Size(max = 50) String name,
        @Size(max = 500) String careerCriteria) {}
