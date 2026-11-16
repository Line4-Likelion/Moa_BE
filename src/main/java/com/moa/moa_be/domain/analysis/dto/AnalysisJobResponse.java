package com.moa.moa_be.domain.analysis.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.moa.moa_be.domain.analysis.entity.AnalysisStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "분석 요청 접수 결과 (202)")
public record AnalysisJobResponse(
        @JsonProperty("job_id") @Schema(description = "분석 작업 ID", example = "5") Long jobId,
        @JsonProperty("capture_id") @Schema(example = "12") Long captureId,
        @Schema(example = "PENDING") AnalysisStatus status,
        @JsonProperty("poll_after_ms") @Schema(description = "이 시간(ms) 뒤에 GET /analysis-jobs 로 조회", example = "2000") long pollAfterMs
) {
}
