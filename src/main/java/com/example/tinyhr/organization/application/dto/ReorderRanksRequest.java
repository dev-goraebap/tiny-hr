package com.example.tinyhr.organization.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record ReorderRanksRequest(
        @NotEmpty List<@NotBlank String> orderedRankIds) {}
