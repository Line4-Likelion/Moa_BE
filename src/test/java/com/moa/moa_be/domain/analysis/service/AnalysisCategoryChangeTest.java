package com.moa.moa_be.domain.analysis.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.moa.moa_be.domain.analysis.config.AnalysisProperties;
import com.moa.moa_be.domain.analysis.dto.CaptureAnalysisResponse;
import com.moa.moa_be.domain.analysis.entity.CategorySource;
import com.moa.moa_be.domain.analysis.entity.ScreenshotAnalysis;
import com.moa.moa_be.domain.category.entity.Category;
import com.moa.moa_be.domain.category.repository.CategoryRepository;
import com.moa.moa_be.domain.category.service.CategoryService;
import com.moa.moa_be.domain.screenshot.entity.Classification;
import com.moa.moa_be.domain.screenshot.entity.ScreenshotClassification;
import com.moa.moa_be.domain.screenshot.repository.ScreenshotClassificationRepository;
import com.moa.moa_be.domain.user.entity.User;
import com.moa.moa_be.global.exception.CustomException;
import com.moa.moa_be.global.exception.GlobalErrorCode;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.json.JsonMapper;

/** PATCH /captures/{id}/category: 실천 사진은 분석 결과, 참고 사진은 캡처의 참고함 카테고리를 바꾼다 */
class AnalysisCategoryChangeTest {

    private static final Long USER_ID = 1L;
    private static final Long CAPTURE_ID = 10L;
    private static final Long CATEGORY_ID = 3L;

    private ScreenshotClassificationRepository captures;
    private AnalysisJobStore jobs;
    private CategoryService categories;
    private AnalysisJobService service;
    private final User user = User.builder().email("user@example.com").password("password").nickname("user").build();

    @BeforeEach
    void setUp() {
        captures = mock(ScreenshotClassificationRepository.class);
        jobs = mock(AnalysisJobStore.class);
        categories = mock(CategoryService.class);
        @SuppressWarnings("unchecked") ObjectProvider<JsonMapper> mapper = mock(ObjectProvider.class);
        service = new AnalysisJobService(mock(AnalysisService.class), jobs, mock(AnalysisPushSender.class),
                categories, mock(CategoryRepository.class), new AnalysisProperties(null, null, 50),
                mock(ThreadPoolTaskExecutor.class), mapper, captures);

        Category food = Category.of(USER_ID, "맛집");
        ReflectionTestUtils.setField(food, "id", CATEGORY_ID);
        when(categories.getMyCategory(USER_ID, CATEGORY_ID)).thenReturn(food);
    }

    @Test
    void referencePhotoChangesCaptureCategoryWithoutAnalysis() {
        ScreenshotClassification reference = new ScreenshotClassification(user, "photo", Classification.REFERENCE);
        when(captures.findOwnedForUpdate(CAPTURE_ID, USER_ID)).thenReturn(Optional.of(reference));

        CaptureAnalysisResponse response = service.changeCategory(USER_ID, CAPTURE_ID, CATEGORY_ID);

        assertEquals(CATEGORY_ID, reference.getCategoryId());
        assertEquals(CATEGORY_ID, response.category().id());
        assertEquals("맛집", response.category().name());
        assertEquals(CategorySource.USER, response.categorySource());
        assertNull(response.jobId());
        assertNull(response.result());
        verifyNoInteractions(jobs);
    }

    @Test
    void practicePhotoKeepsChangingAnalysisCategory() {
        ScreenshotClassification practice = new ScreenshotClassification(user, "photo", Classification.PRACTICE);
        when(captures.findOwnedForUpdate(CAPTURE_ID, USER_ID)).thenReturn(Optional.of(practice));
        when(jobs.changeCategory(USER_ID, CAPTURE_ID, CATEGORY_ID))
                .thenReturn(ScreenshotAnalysis.request(USER_ID, CAPTURE_ID));

        service.changeCategory(USER_ID, CAPTURE_ID, CATEGORY_ID);

        verify(jobs).changeCategory(USER_ID, CAPTURE_ID, CATEGORY_ID);
        assertNull(practice.getCategoryId());
    }

    @Test
    void otherUsersCategoryIs404() {
        when(categories.getMyCategory(anyLong(), anyLong())).thenThrow(new CustomException(GlobalErrorCode.NOT_FOUND));

        CustomException e = assertThrows(CustomException.class, () -> service.changeCategory(USER_ID, CAPTURE_ID, 99L));

        assertEquals(GlobalErrorCode.NOT_FOUND, e.getErrorCode());
        verifyNoInteractions(jobs);
    }
}
