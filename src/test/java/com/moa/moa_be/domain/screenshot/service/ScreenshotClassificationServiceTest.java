package com.moa.moa_be.domain.screenshot.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.moa.moa_be.domain.screenshot.dto.ClassificationBatchRequest;
import com.moa.moa_be.domain.screenshot.entity.Classification;
import com.moa.moa_be.domain.screenshot.entity.ScreenshotClassification;
import com.moa.moa_be.domain.screenshot.repository.ScreenshotClassificationRepository;
import com.moa.moa_be.domain.user.entity.User;
import com.moa.moa_be.domain.user.repository.UserRepository;
import com.moa.moa_be.global.exception.CustomException;
import com.moa.moa_be.global.exception.GlobalErrorCode;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ScreenshotClassificationServiceTest {
    private ScreenshotClassificationRepository repository;
    private UserRepository users;
    private ScreenshotClassificationService service;

    @BeforeEach
    void setUp() {
        repository = mock(ScreenshotClassificationRepository.class);
        users = mock(UserRepository.class);
        service = new ScreenshotClassificationService(repository, users);
    }

    @Test
    void savesDifferentDecisionsForAuthenticatedUser() {
        User user = User.builder().email("user@example.com").password("password").nickname("user").build();
        when(users.findById(1L)).thenReturn(Optional.of(user));
        when(repository.saveAllAndFlush(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        var request = new ClassificationBatchRequest(List.of(
                new ClassificationBatchRequest.Item("photo-1", Classification.PRACTICE),
                new ClassificationBatchRequest.Item("photo-2", Classification.REFERENCE)));

        var result = service.save(1L, request);

        assertEquals(List.of(Classification.PRACTICE, Classification.REFERENCE),
                result.stream().map(r -> r.classification()).toList());
        verify(repository).existsByUserIdAndFileKeyIn(1L, List.of("photo-1", "photo-2"));
    }

    @Test
    void rejectsDuplicateKeysBeforeSavingAnything() {
        var item = new ClassificationBatchRequest.Item("same", Classification.PRACTICE);
        CustomException error = assertThrows(CustomException.class,
                () -> service.save(1L, new ClassificationBatchRequest(List.of(item, item))));
        assertEquals(GlobalErrorCode.DUPLICATE_FILE_KEY, error.getErrorCode());
        verify(repository, never()).saveAllAndFlush(anyList());
    }

    @Test
    void rejectsMoreThanTwenty() {
        var items = IntStream.range(0, 21).mapToObj(i ->
                new ClassificationBatchRequest.Item("photo-" + i, Classification.REFERENCE)).toList();
        CustomException error = assertThrows(CustomException.class,
                () -> service.save(1L, new ClassificationBatchRequest(items)));
        assertEquals(GlobalErrorCode.BATCH_LIMIT_EXCEEDED, error.getErrorCode());
        verifyNoInteractions(repository);
    }

    @Test
    void cannotCancelAnotherUsersClassification() {
        when(repository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.empty());
        CustomException error = assertThrows(CustomException.class, () -> service.cancel(1L, 10L));
        assertEquals(GlobalErrorCode.NOT_FOUND, error.getErrorCode());
        verify(repository, never()).delete(any(ScreenshotClassification.class));
    }

    @Test
    void changesPracticeToReferenceForOwner() {
        User user = User.builder().email("user@example.com").password("password").nickname("user").build();
        ScreenshotClassification record = new ScreenshotClassification(user, "photo-1", Classification.PRACTICE);
        when(repository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(record));

        assertEquals(Classification.REFERENCE, service.change(1L, 10L, Classification.REFERENCE).classification());
        assertEquals(Classification.REFERENCE, record.getClassification());
    }

    @Test
    void rejectsChangingToDelete() {
        User user = User.builder().email("user@example.com").password("password").nickname("user").build();
        ScreenshotClassification record = new ScreenshotClassification(user, "photo-1", Classification.PRACTICE);
        when(repository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(record));

        CustomException error = assertThrows(CustomException.class, () -> service.change(1L, 10L, Classification.DELETE));
        assertEquals(GlobalErrorCode.INVALID_STATE_TRANSITION, error.getErrorCode());
        assertEquals(Classification.PRACTICE, record.getClassification());
    }
}
