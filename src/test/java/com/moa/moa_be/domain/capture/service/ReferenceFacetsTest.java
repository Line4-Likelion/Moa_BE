package com.moa.moa_be.domain.capture.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.moa.moa_be.domain.capture.dto.CaptureListResponse;
import com.moa.moa_be.domain.capture.dto.CaptureListResponse.CategoryCount;
import com.moa.moa_be.domain.category.entity.Category;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class ReferenceFacetsTest {

    private final Category food = category(1L, "맛집");
    private final Category travel = category(2L, "여행");
    private final Category custom = category(5L, "인테리어");
    private final Category other = category(7L, "기타");
    private final List<Category> ordered = List.of(food, travel, custom, other);

    @Test
    void listsEveryCategoryInOrderWithZeroForEmpty() {
        CaptureListResponse.Facets facets = CaptureService.referenceFacets(ordered, Map.of(1L, 2L));

        assertEquals(List.of(new CategoryCount(1L, "맛집", 2), new CategoryCount(2L, "여행", 0),
                new CategoryCount(5L, "인테리어", 0), new CategoryCount(7L, "기타", 0)), facets.categories());
        assertEquals(2, facets.total());
        assertEquals(null, facets.snoozed());
    }

    @Test
    void uncategorizedAndDeletedCategoriesCountAsOther() {
        Map<Long, Long> counts = new HashMap<>();
        counts.put(null, 2L);     // 카테고리 없음 (미분류)
        counts.put(7L, 1L);       // 기타
        counts.put(99L, 1L);      // 지워진 카테고리
        counts.put(2L, 3L);

        CaptureListResponse.Facets facets = CaptureService.referenceFacets(ordered, counts);

        assertEquals(4, facets.categories().get(3).count());
        assertEquals(3, facets.categories().get(1).count());
        assertEquals(7, facets.total());
    }

    private static Category category(Long id, String name) {
        Category category = Category.of(1L, name);
        ReflectionTestUtils.setField(category, "id", id);
        return category;
    }
}
