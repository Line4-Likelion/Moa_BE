package com.moa.moa_be.domain.analysis.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.moa.moa_be.domain.analysis.entity.AnalysisStatus;
import com.moa.moa_be.domain.analysis.entity.CategorySource;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * 캡처 1장의 분석 결과. 실천 상세(GET /captures/{id}, C 파트)에서 이 값을 가져다 쓰면 됨.
 */
@Schema(description = "캡처 분석 결과")
public record CaptureAnalysisResponse(
        @JsonProperty("capture_id") Long captureId,
        @JsonProperty("job_id") Long jobId,
        AnalysisStatus status,
        Category category,
        @JsonProperty("category_source") @Schema(example = "AI") CategorySource categorySource,
        @JsonProperty("analyzed_at") LocalDateTime analyzedAt,
        @Schema(description = "SUCCEEDED 일 때만 값이 있음") AnalysisResult result
) {

    public record Category(Long id, String name) {
    }
}
