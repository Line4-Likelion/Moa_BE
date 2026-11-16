package com.moa.moa_be.domain.category.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.moa.moa_be.domain.category.entity.Category;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "카테고리")
public class CategoryResponse {

    @Schema(description = "카테고리 ID", example = "3")
    private Long id;

    @Schema(description = "카테고리 이름", example = "맛집")
    private String name;

    @JsonProperty("isCustom")
    @Schema(description = "사용자가 직접 만든 카테고리인지", example = "false")
    private boolean custom;

    // TODO: Screenshot/Task 엔티티가 dev에 머지되면 실천 대기(TODO) 개수 집계로 교체
    @JsonProperty("todoCount")
    @Schema(description = "이 카테고리의 실천 대기 항목 수", example = "1")
    private long todoCount;

    public static CategoryResponse of(Category category, long todoCount) {
        return new CategoryResponse(category.getId(), category.getName(), category.isCustom(), todoCount);
    }
}
