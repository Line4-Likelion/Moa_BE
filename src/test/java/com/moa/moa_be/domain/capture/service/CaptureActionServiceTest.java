package com.moa.moa_be.domain.capture.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.moa.moa_be.domain.analysis.dto.AnalysisResult.DeadlineStatus;
import com.moa.moa_be.domain.analysis.entity.ScreenshotAnalysis;
import com.moa.moa_be.domain.capture.dto.CaptureActionRequest;
import com.moa.moa_be.domain.capture.dto.CaptureActionResponse;
import com.moa.moa_be.domain.capture.entity.CaptureAction;
import com.moa.moa_be.domain.capture.entity.CaptureStatus;
import com.moa.moa_be.domain.capture.event.CaptureChangedEvent;
import com.moa.moa_be.domain.capture.exception.FieldValidationException;
import com.moa.moa_be.domain.capture.repository.CaptureQueryRepository;
import com.moa.moa_be.domain.capture.repository.CaptureRow;
import com.moa.moa_be.domain.screenshot.entity.Classification;
import com.moa.moa_be.domain.screenshot.entity.ScreenshotClassification;
import com.moa.moa_be.domain.screenshot.repository.ScreenshotClassificationRepository;
import com.moa.moa_be.domain.user.entity.User;
import com.moa.moa_be.global.exception.CustomException;
import com.moa.moa_be.global.exception.GlobalErrorCode;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

class CaptureActionServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long CAPTURE_ID = 10L;
    private static final LocalDate DEADLINE = LocalDate.of(2026, 11, 30);

    private ScreenshotClassificationRepository classifications;
    private CaptureQueryRepository queries;
    private ApplicationEventPublisher events;
    private CaptureActionService service;
    private ScreenshotClassification capture;
    private ScreenshotAnalysis analysis;

    @BeforeEach
    void setUp() {
        classifications = mock(ScreenshotClassificationRepository.class);
        queries = mock(CaptureQueryRepository.class);
        events = mock(ApplicationEventPublisher.class);
        CaptureAssembler assembler = mock(CaptureAssembler.class);
        service = new CaptureActionService(classifications, queries, assembler, new UndoStore(), events);

        User user = User.builder().email("user@example.com").password("password").nickname("user").build();
        capture = new ScreenshotClassification(user, "media:1", Classification.PRACTICE);
        ReflectionTestUtils.setField(capture, "id", CAPTURE_ID);
        analysis = ScreenshotAnalysis.request(USER_ID, CAPTURE_ID);
        analysis.succeed(DeadlineStatus.FOUND, DEADLINE, 3L, "연남 소바실", true, "{}");

        when(classifications.findOwnedForUpdate(CAPTURE_ID, USER_ID)).thenReturn(Optional.of(capture));
        when(queries.findOne(USER_ID, CAPTURE_ID)).thenAnswer(i -> Optional.of(new CaptureRow(capture, analysis)));
    }

    @Test
    void completeMovesTodoToDoneAndOffersUndo() {
        capture.snooze(LocalDate.now().plusDays(3));

        CaptureActionResponse response = act(CaptureAction.COMPLETE, null, null);

        assertNotNull(capture.getCompletedAt());
        assertNull(capture.getSnoozeUntil());
        assertEquals("UNDO", response.undo().action());
        assertTrue(response.undo().token().startsWith("und_"));
        assertEquals("+09:00", response.undo().expiresAt().getOffset().getId());
        assertEvent(CaptureStatus.TODO, CaptureStatus.DONE);
    }

    @Test
    void snoozeWithoutDateDefaultsToSameWeekdayNextWeek() {
        CaptureActionResponse response = act(CaptureAction.SNOOZE, null, null);

        assertEquals(CaptureTimes.today().plusWeeks(1), capture.getSnoozeUntil());
        assertNotNull(response.undo());
        verify(events, never()).publishEvent(any(Object.class)); // 상태는 TODO 그대로
    }

    @Test
    void snoozeKeepsRequestedDate() {
        LocalDate until = CaptureTimes.today().plusDays(2);
        act(CaptureAction.SNOOZE, until, null);
        assertEquals(until, capture.getSnoozeUntil());
    }

    @Test
    void snoozeBeforeTodayIs400WithField() {
        FieldValidationException e = assertThrows(FieldValidationException.class,
                () -> act(CaptureAction.SNOOZE, CaptureTimes.today().minusDays(1), null));
        assertTrue(e.getFields().containsKey("snooze_until"));
        assertNull(capture.getSnoozeUntil());
    }

    @Test
    void dismissMovesToReferenceAndKeepsAnalysisForUndo() {
        act(CaptureAction.DISMISS, null, null);

        assertEquals(Classification.REFERENCE, capture.getClassification());
        assertTrue(capture.isDismissed());
        assertEquals(3L, capture.getCategoryId()); // 실천일 때의 카테고리를 참고함에서도 유지
        assertEvent(CaptureStatus.TODO, CaptureStatus.REFERENCE);
    }

    @Test
    void undoWithTokenRestoresPreviousState() {
        String token = act(CaptureAction.DISMISS, null, null).undo().token();

        CaptureActionResponse response = act(CaptureAction.UNDO, null, token);

        assertNull(response.undo());
        assertEquals(Classification.PRACTICE, capture.getClassification());
        assertFalse(capture.isDismissed());
        assertNull(capture.getCategoryId());
    }

    @Test
    void undoAfterCompleteRestoresSnooze() {
        LocalDate until = CaptureTimes.today().plusDays(2);
        capture.snooze(until);
        String token = act(CaptureAction.COMPLETE, null, null).undo().token();

        act(CaptureAction.UNDO, null, token);

        assertNull(capture.getCompletedAt());
        assertEquals(until, capture.getSnoozeUntil());
    }

    @Test
    void undoWithoutTokenIs400() {
        FieldValidationException e = assertThrows(FieldValidationException.class,
                () -> act(CaptureAction.UNDO, null, " "));
        assertTrue(e.getFields().containsKey("token"));
    }

    @Test
    void undoWithWrongTokenIs409() {
        act(CaptureAction.COMPLETE, null, null);
        assertError(GlobalErrorCode.INVALID_STATE_TRANSITION, () -> act(CaptureAction.UNDO, null, "und_wrong"));
        assertNotNull(capture.getCompletedAt());
    }

    @Test
    void reopenOnTodoIsNoChange() {
        CaptureActionResponse response = act(CaptureAction.REOPEN, null, null);

        assertNull(response.undo());
        assertNull(capture.getCompletedAt());
        verify(events, never()).publishEvent(any(Object.class));
    }

    @Test
    void reopenMovesDoneBackToTodo() {
        act(CaptureAction.COMPLETE, null, null);
        act(CaptureAction.REOPEN, null, null);
        assertNull(capture.getCompletedAt());
    }

    @Test
    void completeWhileAnalyzingIs409() {
        analysis = ScreenshotAnalysis.request(USER_ID, CAPTURE_ID);
        assertError(GlobalErrorCode.INVALID_STATE_TRANSITION, () -> act(CaptureAction.COMPLETE, null, null));
    }

    @Test
    void otherUsersCaptureIs404() {
        when(classifications.findOwnedForUpdate(anyLong(), anyLong())).thenReturn(Optional.empty());
        assertError(GlobalErrorCode.NOT_FOUND, () -> service.act(2L, CAPTURE_ID,
                new CaptureActionRequest(CaptureAction.COMPLETE, null, null)));
    }

    private CaptureActionResponse act(CaptureAction action, LocalDate snoozeUntil, String token) {
        return service.act(USER_ID, CAPTURE_ID, new CaptureActionRequest(action, snoozeUntil, token));
    }

    private void assertEvent(CaptureStatus before, CaptureStatus after) {
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(events).publishEvent(captor.capture());
        CaptureChangedEvent event = (CaptureChangedEvent) captor.getValue();
        assertEquals(before, event.previousStatus());
        assertEquals(after, event.currentStatus());
        assertEquals(DEADLINE, event.currentDeadline());
    }

    private static void assertError(GlobalErrorCode code, org.junit.jupiter.api.function.Executable executable) {
        CustomException e = assertThrows(CustomException.class, executable);
        assertEquals(code, e.getErrorCode());
    }
}
