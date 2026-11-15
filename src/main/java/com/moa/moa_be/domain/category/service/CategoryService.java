package com.moa.moa_be.domain.category.service;

import com.moa.moa_be.domain.category.dto.CategoryCreateRequest;
import com.moa.moa_be.domain.category.dto.CategoryListResponse;
import com.moa.moa_be.domain.category.dto.CategoryResponse;
import com.moa.moa_be.domain.category.entity.Category;
import com.moa.moa_be.domain.category.entity.DefaultCategory;
import com.moa.moa_be.domain.category.repository.CategoryRepository;
import com.moa.moa_be.global.exception.CustomException;
import com.moa.moa_be.global.exception.GlobalErrorCode;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository categoryRepository;

    /** B-3: 기본 카테고리 → 사용자 카테고리(만든 순) → 기타 */
    @Transactional
    public CategoryListResponse getCategories(Long userId) {
        // 기본 카테고리 생성 기능 이전에 가입한 계정이면 여기서 채워 줌
        createDefaultCategories(userId);
        List<CategoryResponse> categories = categoryRepository.findAllByUserId(userId).stream()
                .sorted(displayOrder())
                .map(category -> CategoryResponse.of(category, 0L)) // TODO: todo_count 집계
                .toList();
        return new CategoryListResponse(categories);
    }

    /** B-5: 카테고리 추가. 같은 이름이 있으면 409 DUPLICATE_NAME */
    @Transactional
    public CategoryResponse createCategory(Long userId, CategoryCreateRequest request) {
        String name = request.getName().trim();
        if (name.isEmpty()) {
            throw new CustomException(GlobalErrorCode.VALIDATION_ERROR);
        }
        if (categoryRepository.existsByUserIdAndName(userId, name)) {
            throw new CustomException(GlobalErrorCode.DUPLICATE_NAME);
        }
        try {
            Category saved = categoryRepository.saveAndFlush(Category.of(userId, name));
            return CategoryResponse.of(saved, 0L);
        } catch (DataIntegrityViolationException e) {
            // 동시에 같은 이름으로 두 번 요청된 경우 (UNIQUE 제약 위반)
            throw new CustomException(GlobalErrorCode.DUPLICATE_NAME);
        }
    }

    /**
     * 회원가입 직후 호출: 기본 카테고리 7개 생성.
     * 인증 파트 회원가입 로직에서 categoryService.createDefaultCategories(user.getId()) 한 줄만 추가하면 됨.
     */
    @Transactional
    public void createDefaultCategories(Long userId) {
        List<Category> defaults = Arrays.stream(DefaultCategory.values())
                .filter(c -> !categoryRepository.existsByUserIdAndName(userId, c.getDisplayName()))
                .map(c -> Category.of(userId, c.getDisplayName()))
                .toList();
        categoryRepository.saveAll(defaults);
    }

    /** 회원 탈퇴 시 호출 */
    @Transactional
    public void deleteAllByUserId(Long userId) {
        categoryRepository.deleteAllByUserId(userId);
    }

    /** AI 분석(B-2)에서 쓸 카테고리 이름 목록 */
    @Transactional
    public List<String> getCategoryNames(Long userId) {
        createDefaultCategories(userId);
        return categoryRepository.findAllByUserId(userId).stream()
                .sorted(displayOrder())
                .map(Category::getName)
                .toList();
    }

    /** 다른 도메인에서 카테고리를 찾을 때 사용 (내 카테고리가 아니면 404) */
    public Category getMyCategory(Long userId, Long categoryId) {
        return categoryRepository.findByIdAndUserId(categoryId, userId)
                .orElseThrow(() -> new CustomException(GlobalErrorCode.NOT_FOUND));
    }

    private Comparator<Category> displayOrder() {
        return Comparator
                .comparingInt((Category c) -> c.isOther() ? 2 : (c.isCustom() ? 1 : 0))
                .thenComparingInt(c -> DefaultCategory.orderOf(c.getName()))
                .thenComparing(Category::getId);
    }
}
