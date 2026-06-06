package com.example.tinyhr.organization.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record CreatePositionRequest(
        @NotBlank @Size(max = 50) String name,
        @PositiveOrZero int displayOrder,
        @Size(max = 500) String careerCriteria) {}
