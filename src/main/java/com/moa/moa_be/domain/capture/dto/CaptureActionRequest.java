package com.moa.moa_be.domain.capture.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.moa.moa_be.domain.capture.entity.CaptureAction;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

@Schema(description = "상태 변경 요청")
public record CaptureActionRequest(
        @NotNull CaptureAction action,
        @JsonProperty("snooze_until")
        @Schema(description = "SNOOZE 일 때만. 없으면 다음 주 같은 요일", example = "2026-11-12") LocalDate snoozeUntil,
        @Schema(description = "UNDO 일 때 직전 응답의 undo.token", example = "und_7xk2") String token
) {
}
