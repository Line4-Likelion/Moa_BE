package com.moa.moa_be.domain.category.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "카테고리 목록")
public class CategoryListResponse {

    @Schema(description = "기본 카테고리 → 사용자 카테고리 → 기타 순")
    private List<CategoryResponse> categories;
}
