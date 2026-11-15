package com.moa.moa_be.domain.capture.service;

import com.moa.moa_be.domain.analysis.dto.AnalysisResult;
import com.moa.moa_be.domain.capture.dto.CaptureDetailResponse;
import com.moa.moa_be.domain.capture.dto.FactsUpdateRequest;
import com.moa.moa_be.domain.capture.dto.FactsUpdateRequest.FactPatch;
import com.moa.moa_be.domain.capture.entity.FactOverride;
import com.moa.moa_be.domain.capture.event.CaptureChangedEvent;
import com.moa.moa_be.domain.capture.exception.FieldValidationException;
import com.moa.moa_be.domain.capture.repository.CaptureQueryRepository;
import com.moa.moa_be.domain.capture.repository.CaptureRow;
import com.moa.moa_be.domain.capture.repository.FactOverrideRepository;
import com.moa.moa_be.domain.screenshot.repository.ScreenshotClassificationRepository;
import com.moa.moa_be.global.exception.CustomException;
import com.moa.moa_be.global.exception.GlobalErrorCode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * PATCH /captures/{id}/facts (C-07). 보낸 key 만 수정, value 가 null 이면 AI 값으로 되돌림.
 * key 가 deadline 이면 고친 날짜가 정렬·알림 기준 마감기한이 된다 (null 이면 AI 마감기한으로).
 */
@Service
@RequiredArgsConstructor
public class FactService {

    static final String DEADLINE_KEY = FactOverride.DEADLINE_KEY;

    private static final Pattern DATE_FORMAT = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");
    private static final DateTimeFormatter STRICT_DATE =
            DateTimeFormatter.ofPattern("uuuu-MM-dd").withResolverStyle(ResolverStyle.STRICT);

    private final ScreenshotClassificationRepository classificationRepository;
    private final CaptureQueryRepository captureQueryRepository;
    private final FactOverrideRepository factOverrideRepository;
    private final CaptureAssembler assembler;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public CaptureDetailResponse update(Long userId, Long captureId, FactsUpdateRequest request) {
        classificationRepository.findOwnedForUpdate(captureId, userId)
                .orElseThrow(() -> new CustomException(GlobalErrorCode.NOT_FOUND));
        CaptureRow row = captureQueryRepository.findOne(userId, captureId)
                .orElseThrow(() -> new CustomException(GlobalErrorCode.NOT_FOUND));
        AnalysisResult result = assembler.readResult(row);
        if (result == null) {
            // 분석 중·실패·참고 항목에는 고칠 핵심 정보가 없음
            throw new CustomException(GlobalErrorCode.INVALID_STATE_TRANSITION);
        }

        Map<String, String> changes = validate(request, result);
        LocalDate deadlineBefore = assembler.effectiveDeadline(row);

        // 마감기한도 다른 핵심 정보처럼 수정 기록으로 저장 (AI 원본·분석 결과 테이블은 그대로)
        changes.forEach((key, value) -> changeFact(row, key,
                DEADLINE_KEY.equals(key) && value != null ? LocalDate.parse(value, STRICT_DATE).toString() : value));

        LocalDate deadlineAfter = assembler.effectiveDeadline(row);
        if (!Objects.equals(deadlineBefore, deadlineAfter)) {
            eventPublisher.publishEvent(new CaptureChangedEvent(userId, captureId, row.status(), row.status(),
                    deadlineBefore, deadlineAfter));
        }
        return assembler.toDetail(userId, row);
    }

    /** @return key → 새 값 (null 이면 되돌리기). 하나라도 잘못되면 key 별 사유와 함께 400 */
    private static Map<String, String> validate(FactsUpdateRequest request, AnalysisResult result) {
        if (request == null || request.facts() == null || request.facts().isEmpty()) {
            throw FieldValidationException.of("facts", "수정할 항목을 보내주세요");
        }
        Set<String> keys = result.facts() == null ? Set.of()
                : result.facts().stream().map(AnalysisResult.Fact::key).collect(Collectors.toSet());
        Map<String, String> changes = new LinkedHashMap<>();
        Map<String, String> errors = new LinkedHashMap<>();
        for (FactPatch patch : request.facts()) {
            String key = patch == null ? null : patch.key();
            if (key == null || key.isBlank()) {
                errors.put("facts", "key 를 입력해주세요");
                continue;
            }
            if (changes.containsKey(key) || errors.containsKey(key)) {
                errors.put(key, "같은 항목을 두 번 보낼 수 없어요");
                continue;
            }
            String value = patch.value();
            String reason = DEADLINE_KEY.equals(key) ? checkDate(value)
                    : !keys.contains(key) ? "없는 항목이에요"
                    : checkText(value);
            if (reason != null) {
                errors.put(key, reason);
            } else {
                changes.put(key, value == null ? null : value.trim());
            }
        }
        if (!errors.isEmpty()) {
            throw new FieldValidationException(errors);
        }
        return changes;
    }

    private static String checkDate(String value) {
        if (value == null) {
            return null;
        }
        if (!DATE_FORMAT.matcher(value.trim()).matches()) {
            return "YYYY-MM-DD 형식으로 입력해주세요";
        }
        try {
            LocalDate.parse(value.trim(), STRICT_DATE);
            return null;
        } catch (DateTimeParseException e) {
            return "존재하지 않는 날짜예요";
        }
    }

    private static String checkText(String value) {
        if (value == null) {
            return null;
        }
        if (value.isBlank()) {
            return "값을 입력해주세요. AI 값으로 되돌리려면 null 을 보내주세요";
        }
        if (value.trim().length() > FactOverride.MAX_VALUE_LENGTH) {
            return FactOverride.MAX_VALUE_LENGTH + "자 이하로 입력해주세요";
        }
        return null;
    }

    private void changeFact(CaptureRow row, String key, String value) {
        Long captureId = row.capture().getId();
        FactOverride existing = factOverrideRepository.findByCaptureIdAndFactKey(captureId, key).orElse(null);
        if (value == null) {
            if (existing != null) {
                factOverrideRepository.delete(existing);
            }
        } else if (existing != null) {
            existing.change(value);
        } else {
            factOverrideRepository.save(new FactOverride(row.capture(), key, value));
        }
    }
}
