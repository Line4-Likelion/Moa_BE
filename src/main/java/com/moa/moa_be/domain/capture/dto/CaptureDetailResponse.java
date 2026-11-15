package com.moa.moa_be.domain.capture.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.moa.moa_be.domain.analysis.entity.CategorySource;
import com.moa.moa_be.domain.capture.entity.CaptureStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

@Schema(description = "캡처 상세형 = 요약형 + 핵심 정보·링크·가이드·체크리스트")
public record CaptureDetailResponse(
        @Schema(example = "12") Long id,
        @JsonProperty("file_key") String fileKey,
        CaptureStatus status,
        CaptureParts.CategoryInfo category,
        String title,
        String summary,
        CaptureParts.DeadlineInfo deadline,
        @JsonProperty("checklist_progress") CaptureParts.Progress checklistProgress,
        @JsonProperty("snooze_until") LocalDate snoozeUntil,
        @JsonProperty("folder_id") Long folderId,
        boolean dismissed,
        @JsonProperty("taken_at") OffsetDateTime takenAt,
        @JsonProperty("created_at") OffsetDateTime createdAt,
        @JsonProperty("status_changed_at") OffsetDateTime statusChangedAt,
        @JsonProperty("category_source") CategorySource categorySource,
        List<CaptureParts.FactItem> facts,
        List<CaptureParts.LinkItem> links,
        CaptureParts.GuideInfo guide,
        List<CaptureParts.ChecklistItem> checklist,
        @JsonProperty("next_alarm_at") @Schema(description = "알림 모듈 값 (연동 전까지 null)") OffsetDateTime nextAlarmAt,
        @Schema(description = "status 가 FAILED 일 때 실패 사유") CaptureParts.ErrorBody error
) {
}
