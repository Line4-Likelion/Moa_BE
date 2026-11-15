package com.moa.moa_be.domain.analysis.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "카테고리 수동 변경 요청 (B-4)")
public record CategoryChangeRequest(
        @NotNull(message = "변경할 카테고리를 선택해 주세요.")
        @Schema(example = "3") Long categoryId
) {
}
