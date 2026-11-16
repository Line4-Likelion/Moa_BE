package com.moa.moa_be.domain.category.controller;

import com.moa.moa_be.domain.category.dto.CategoryCreateRequest;
import com.moa.moa_be.domain.category.dto.CategoryListResponse;
import com.moa.moa_be.domain.category.dto.CategoryResponse;
import com.moa.moa_be.domain.category.service.CategoryService;
import com.moa.moa_be.global.common.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Category", description = "카테고리 API (B-3, B-5)")
@RestController
@RequiredArgsConstructor
@RequestMapping("/categories")
public class CategoryController {

    private final CategoryService categoryService;

    // JwtAuthenticationFilter가 principal에 userId(Long)를 넣어 두므로 바로 주입받음
    @Operation(summary = "카테고리 목록 조회", description = "기본 카테고리 + 사용자 카테고리 + 기타, 각 실천 대기 개수 포함")
    @GetMapping
    public ResponseEntity<BaseResponse<CategoryListResponse>> getCategories(
            @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(BaseResponse.success(categoryService.getCategories(userId)));
    }

    @Operation(summary = "카테고리 추가", description = "같은 이름이 이미 있으면 409 DUPLICATE_NAME")
    @PostMapping
    public ResponseEntity<BaseResponse<CategoryResponse>> createCategory(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody CategoryCreateRequest request) {
        CategoryResponse response = categoryService.createCategory(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.success(201, "카테고리가 추가되었습니다.", response));
    }
}
