package com.example.tinyhr.approval.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 관리자 강제 취소 입력(사유 필수). */
public record AdminCancelRequest(@NotBlank @Size(max = 2000) String reason) {}
