package com.moa.moa_be.domain.capture.service;

import com.moa.moa_be.domain.analysis.entity.ScreenshotAnalysis;
import com.moa.moa_be.domain.capture.dto.CaptureActionRequest;
import com.moa.moa_be.domain.capture.dto.CaptureActionResponse;
import com.moa.moa_be.domain.capture.entity.CaptureAction;
import com.moa.moa_be.domain.capture.entity.CaptureStatus;
import com.moa.moa_be.domain.capture.event.CaptureChangedEvent;
import com.moa.moa_be.domain.capture.exception.FieldValidationException;
import com.moa.moa_be.domain.capture.repository.CaptureQueryRepository;
import com.moa.moa_be.domain.capture.repository.CaptureRow;
import com.moa.moa_be.domain.capture.service.CaptureStateMachine.Outcome;
import com.moa.moa_be.domain.screenshot.entity.Classification;
import com.moa.moa_be.domain.screenshot.entity.ScreenshotClassification;
import com.moa.moa_be.domain.screenshot.repository.ScreenshotClassificationRepository;
import com.moa.moa_be.global.exception.CustomException;
import com.moa.moa_be.global.exception.GlobalErrorCode;
import java.time.Instant;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** POST /captures/{id}/actions — 완료(C-08)·보류(C-09)·안함(C-10)·되돌리기/다시 실천(C-11) */
@Service
@RequiredArgsConstructor
public class CaptureActionService {

    private final ScreenshotClassificationRepository classificationRepository;
    private final CaptureQueryRepository captureQueryRepository;
    private final CaptureAssembler assembler;
    private final UndoStore undoStore;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public CaptureActionResponse act(Long userId, Long captureId, CaptureActionRequest request) {
        // 같은 캡처에 동시에 들어온 요청은 순서대로 처리
        ScreenshotClassification capture = classificationRepository.findOwnedForUpdate(captureId, userId)
                .filter(c -> c.getClassification() != Classification.DELETE)
                .orElseThrow(() -> new CustomException(GlobalErrorCode.NOT_FOUND));
        CaptureRow row = captureQueryRepository.findOne(userId, captureId)
                .orElseThrow(() -> new CustomException(GlobalErrorCode.NOT_FOUND));
        CaptureStatus before = row.status();
        CaptureAction action = request.action();

        UndoStore.Entry undo = null;
        if (action == CaptureAction.UNDO) {
            if (request.token() == null || request.token().isBlank()) {
                throw FieldValidationException.of("token", "되돌리기 토큰을 함께 보내주세요");
            }
            capture.restore(undoStore.consume(userId, captureId, request.token()).previous());
        } else {
            if (CaptureStateMachine.decide(before, action) == Outcome.NO_CHANGE) {
                return new CaptureActionResponse(assembler.toSummary(userId, row), null);
            }
            var previous = capture.snapshot();
            apply(capture, action, request.snoozeUntil());
            undo = undoStore.issue(userId, captureId, action, previous);
        }

        CaptureStatus after = CaptureStatus.of(capture, row.analysis());
        if (before != after) {
            LocalDate deadline = deadlineOf(row.analysis());
            eventPublisher.publishEvent(
                    new CaptureChangedEvent(userId, captureId, before, after, deadline, deadline));
        }
        return new CaptureActionResponse(assembler.toSummary(userId, row),
                undo == null ? null
                        : new CaptureActionResponse.Undo(CaptureAction.UNDO.name(), undo.token(),
                        CaptureTimes.toKst(undo.expiresAt())));
    }

    private static void apply(ScreenshotClassification capture, CaptureAction action, LocalDate snoozeUntil) {
        Instant now = Instant.now();
        switch (action) {
            case COMPLETE -> capture.complete(now);
            case SNOOZE -> capture.snooze(snoozeDate(snoozeUntil));
            // 분석 내용은 되돌리기 시간이 지난 뒤 DismissCleanupScheduler 가 삭제
            case DISMISS -> capture.dismiss(now);
            case REOPEN -> capture.reopen(now);
            case UNDO -> throw new IllegalStateException("UNDO 는 따로 처리");
        }
    }

    /** 날짜가 없으면 다음 주 같은 요일, 오늘 이전이면 400 */
    static LocalDate snoozeDate(LocalDate requested) {
        LocalDate today = CaptureTimes.today();
        if (requested == null) {
            return today.plusWeeks(1);
        }
        if (requested.isBefore(today)) {
            throw FieldValidationException.of("snooze_until", "오늘 이후 날짜를 선택해주세요");
        }
        return requested;
    }

    private static LocalDate deadlineOf(ScreenshotAnalysis analysis) {
        return analysis == null ? null : analysis.getDeadlineDate();
    }
}
