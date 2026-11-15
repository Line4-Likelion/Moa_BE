package com.moa.moa_be.domain.capture.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "체크 결과. 전부 체크해도 자동 완료하지 않음")
public record ChecklistCheckResponse(
        CaptureParts.ChecklistItem item,
        CaptureParts.Progress progress,
        @JsonProperty("all_done") boolean allDone
) {
}
