package com.moa.moa_be.domain.capture.service;

import com.moa.moa_be.domain.analysis.repository.ScreenshotAnalysisRepository;
import com.moa.moa_be.domain.capture.dto.CaptureDetailResponse;
import com.moa.moa_be.domain.capture.dto.CaptureListResponse;
import com.moa.moa_be.domain.capture.dto.CaptureSummaryResponse;
import com.moa.moa_be.domain.capture.entity.CaptureStatus;
import com.moa.moa_be.domain.capture.event.CaptureChangedEvent;
import com.moa.moa_be.domain.capture.exception.FieldValidationException;
import com.moa.moa_be.domain.capture.repository.CaptureListCondition;
import com.moa.moa_be.domain.capture.repository.CaptureListCondition.Sort;
import com.moa.moa_be.domain.capture.repository.CaptureQueryRepository;
import com.moa.moa_be.domain.capture.repository.CaptureRow;
import com.moa.moa_be.domain.screenshot.entity.ScreenshotClassification;
import com.moa.moa_be.domain.screenshot.repository.ScreenshotClassificationRepository;
import com.moa.moa_be.global.exception.CustomException;
import com.moa.moa_be.global.exception.GlobalErrorCode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 실천함·참고함 목록(C-12, C-13), 상세(C-01~C-06), 삭제 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CaptureService {

    static final int DEFAULT_LIMIT = 20;
    static final int MAX_LIMIT = 100;
    private static final String CURSOR_PREFIX = "o:";

    private final CaptureQueryRepository captureQueryRepository;
    private final ScreenshotClassificationRepository classificationRepository;
    private final ScreenshotAnalysisRepository analysisRepository;
    private final CaptureAssembler assembler;
    private final UndoStore undoStore;
    private final ApplicationEventPublisher eventPublisher;

    /** GET /captures */
    public CaptureListResponse list(Long userId, String status, Boolean snoozed, String sort, String cursor,
                                    Integer limit, LocalDate completedOn) {
        Map<String, String> errors = new LinkedHashMap<>();
        CaptureStatus tab = parseStatus(status, errors);
        Sort order = parseSort(sort, tab, errors);
        int offset = parseCursor(cursor, errors);
        int size = limit == null ? DEFAULT_LIMIT : limit;
        if (size < 1 || size > MAX_LIMIT) {
            errors.put("limit", "1~" + MAX_LIMIT + " 사이로 입력해주세요");
        }
        if (!errors.isEmpty()) {
            throw new FieldValidationException(errors);
        }

        CaptureListCondition condition = new CaptureListCondition(tab, snoozed, CaptureTimes.today(),
                completedOn == null ? null : CaptureTimes.startOf(completedOn),
                completedOn == null ? null : CaptureTimes.startOf(completedOn.plusDays(1)),
                order);

        List<CaptureRow> rows = captureQueryRepository.findPage(userId, condition, offset, size + 1);
        boolean hasNext = rows.size() > size;
        List<CaptureSummaryResponse> items = assembler.toSummaries(userId, hasNext ? rows.subList(0, size) : rows);
        String nextCursor = hasNext ? encodeCursor(offset + size) : null;

        CaptureListResponse.Facets facets = null;
        if (tab != CaptureStatus.REFERENCE) {
            CaptureListCondition whole = condition.tabOnly();
            facets = new CaptureListResponse.Facets(
                    captureQueryRepository.count(userId, whole.withSnoozed(true)),
                    captureQueryRepository.count(userId, whole));
        }
        return new CaptureListResponse(items, nextCursor, facets);
    }

    /** GET /captures/{id} */
    public CaptureDetailResponse detail(Long userId, Long captureId) {
        return assembler.toDetail(userId, findRow(userId, captureId));
    }

    /** DELETE /captures/{id} — 앱이 기기에서 이미지를 지운 뒤 호출. 서버 기록 삭제 */
    @Transactional
    public void delete(Long userId, Long captureId) {
        ScreenshotClassification capture = classificationRepository.findOwnedForUpdate(captureId, userId)
                .orElseThrow(() -> new CustomException(GlobalErrorCode.NOT_FOUND));
        CaptureRow before = captureQueryRepository.findOne(userId, captureId).orElse(null);

        analysisRepository.deleteByCaptureId(captureId);
        undoStore.invalidate(captureId);
        // 체크리스트·핵심 정보 수정 기록은 DB FK(on delete cascade)로 함께 삭제
        classificationRepository.delete(capture);

        if (before != null && before.status() != null) {
            eventPublisher.publishEvent(new CaptureChangedEvent(userId, captureId, before.status(), null,
                    before.analysis() == null ? null : before.analysis().getDeadlineDate(), null));
        }
    }

    CaptureRow findRow(Long userId, Long captureId) {
        return captureQueryRepository.findOne(userId, captureId)
                .orElseThrow(() -> new CustomException(GlobalErrorCode.NOT_FOUND));
    }

    private static CaptureStatus parseStatus(String status, Map<String, String> errors) {
        if (status == null || status.isBlank()) {
            return CaptureStatus.TODO;
        }
        try {
            CaptureStatus parsed = CaptureStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
            if (parsed == CaptureStatus.TODO || parsed == CaptureStatus.DONE || parsed == CaptureStatus.REFERENCE) {
                return parsed;
            }
        } catch (IllegalArgumentException ignored) {
            // 아래에서 처리
        }
        errors.put("status", "TODO, DONE, REFERENCE 중 하나로 입력해주세요");
        return CaptureStatus.TODO;
    }

    private static Sort parseSort(String sort, CaptureStatus tab, Map<String, String> errors) {
        if (sort == null || sort.isBlank()) {
            return tab == CaptureStatus.TODO ? Sort.DEADLINE : Sort.RECENT;
        }
        try {
            return Sort.valueOf(sort.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            errors.put("sort", "deadline, recent 중 하나로 입력해주세요");
            return Sort.RECENT;
        }
    }

    /** cursor 는 다음 페이지 시작 위치를 감춘 값 */
    private static int parseCursor(String cursor, Map<String, String> errors) {
        if (cursor == null || cursor.isBlank()) {
            return 0;
        }
        try {
            String decoded = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
            if (decoded.startsWith(CURSOR_PREFIX)) {
                int offset = Integer.parseInt(decoded.substring(CURSOR_PREFIX.length()));
                if (offset >= 0) {
                    return offset;
                }
            }
        } catch (IllegalArgumentException ignored) {
            // 아래에서 처리
        }
        errors.put("cursor", "올바르지 않은 cursor 예요");
        return 0;
    }

    private static String encodeCursor(int offset) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString((CURSOR_PREFIX + offset).getBytes(StandardCharsets.UTF_8));
    }
}
