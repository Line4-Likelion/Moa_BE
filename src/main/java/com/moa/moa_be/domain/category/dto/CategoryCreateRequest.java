package com.moa.moa_be.domain.category.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "카테고리 추가 요청")
public class CategoryCreateRequest {

    @NotBlank(message = "카테고리 이름을 입력해 주세요.")
    @Size(max = 50, message = "카테고리 이름은 50자 이하여야 합니다.")
    @Schema(description = "카테고리 이름", example = "생활")
    private String name;
}
