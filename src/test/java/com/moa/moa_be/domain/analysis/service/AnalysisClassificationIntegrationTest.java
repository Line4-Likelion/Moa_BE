package com.moa.moa_be.domain.analysis.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.moa.moa_be.domain.analysis.config.AnalysisProperties;
import com.moa.moa_be.domain.analysis.entity.ScreenshotAnalysis;
import com.moa.moa_be.domain.capture.repository.CaptureQueryRepository;
import com.moa.moa_be.domain.capture.service.CaptureAssembler;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import tools.jackson.databind.json.JsonMapper;

class AnalysisClassificationIntegrationTest {
    private ScreenshotClassificationRepository captures;
    private AnalysisService analysis;
    private AnalysisJobStore jobs;
    private ThreadPoolTaskExecutor executor;
    private AnalysisJobService service;
    private final MockMultipartFile image = new MockMultipartFile("image", "photo.png", "image/png", new byte[]{1});

    @BeforeEach
    void setUp() {
        captures = mock(ScreenshotClassificationRepository.class);
        analysis = mock(AnalysisService.class);
        jobs = mock(AnalysisJobStore.class);
        executor = mock(ThreadPoolTaskExecutor.class);
        @SuppressWarnings("unchecked") ObjectProvider<JsonMapper> mapper = mock(ObjectProvider.class);
        service = new AnalysisJobService(analysis, jobs, mock(AnalysisPushSender.class),
                mock(CategoryService.class), mock(CategoryRepository.class),
                new AnalysisProperties(null, null, 50), executor, mapper, captures,
                mock(CaptureQueryRepository.class), mock(CaptureAssembler.class));
    }

    @Test
    void rejectsAnotherUsersCaptureBeforeReadingImage() {
        when(captures.findOwnedForUpdate(10L, 1L)).thenReturn(Optional.empty());
        CustomException error = assertThrows(CustomException.class, () -> service.request(1L, 10L, image));
        assertEquals(GlobalErrorCode.NOT_FOUND, error.getErrorCode());
        verifyNoInteractions(analysis, jobs, executor);
    }

    @Test
    void rejectsReferenceCapture() {
        var user = User.builder().email("user@example.com").password("password").nickname("user").build();
        when(captures.findOwnedForUpdate(10L, 1L))
                .thenReturn(Optional.of(new ScreenshotClassification(user, "photo", Classification.REFERENCE)));
        CustomException error = assertThrows(CustomException.class, () -> service.request(1L, 10L, image));
        assertEquals(GlobalErrorCode.INVALID_STATE_TRANSITION, error.getErrorCode());
        verifyNoInteractions(analysis, jobs, executor);
    }

    @Test
    void startsPracticeAnalysisOnlyAfterCommit() {
        var user = User.builder().email("user@example.com").password("password").nickname("user").build();
        when(captures.findOwnedForUpdate(10L, 1L))
                .thenReturn(Optional.of(new ScreenshotClassification(user, "photo", Classification.PRACTICE)));
        when(analysis.readImage(image)).thenReturn(new byte[]{1});
        ScreenshotAnalysis job = mock(ScreenshotAnalysis.class);
        when(job.getId()).thenReturn(99L);
        when(jobs.createOrReset(1L, 10L, 50)).thenReturn(job);

        TransactionSynchronizationManager.initSynchronization();
        try {
            assertEquals(99L, service.request(1L, 10L, image).jobId());
            verifyNoInteractions(executor);
            TransactionSynchronizationManager.getSynchronizations().forEach(sync -> sync.afterCommit());
            verify(executor).execute(any(Runnable.class));
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }
}
