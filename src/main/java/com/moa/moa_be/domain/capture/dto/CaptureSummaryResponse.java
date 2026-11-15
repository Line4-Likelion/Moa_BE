package com.moa.moa_be.domain.capture.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.moa.moa_be.domain.capture.entity.CaptureStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Schema(description = "캡처 요약형 (목록)")
public record CaptureSummaryResponse(
        @Schema(example = "12") Long id,
        @JsonProperty("file_key") @Schema(description = "기기 사진 식별자. 이미지는 서버에 없음") String fileKey,
        CaptureStatus status,
        CaptureParts.CategoryInfo category,
        String title,
        String summary,
        CaptureParts.DeadlineInfo deadline,
        @JsonProperty("checklist_progress") CaptureParts.Progress checklistProgress,
        @JsonProperty("snooze_until") @Schema(description = "보류 기한. 오늘 이후면 보류 중") LocalDate snoozeUntil,
        @JsonProperty("folder_id") Long folderId,
        @Schema(description = "안함으로 참고함에 온 항목") boolean dismissed,
        @JsonProperty("taken_at") @Schema(description = "촬영 시각 (서버에 아직 저장하지 않아 null)") OffsetDateTime takenAt,
        @JsonProperty("created_at") OffsetDateTime createdAt,
        @JsonProperty("status_changed_at") OffsetDateTime statusChangedAt
) {
}
