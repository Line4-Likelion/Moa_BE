package com.moa.moa_be.domain.capture.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;

@Schema(description = "상태 변경 결과")
public record CaptureActionResponse(
        CaptureSummaryResponse capture,
        @Schema(description = "5초 안에 되돌릴 수 있으면 값이 있음. UNDO 응답·변경 없음이면 null") Undo undo
) {

    public record Undo(@Schema(example = "UNDO") String action,
                       @Schema(example = "und_7xk2") String token,
                       @JsonProperty("expires_at") OffsetDateTime expiresAt) {
    }
}
