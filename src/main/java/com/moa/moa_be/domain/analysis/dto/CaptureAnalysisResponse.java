package com.moa.moa_be.domain.analysis.dto;

import com.moa.moa_be.domain.analysis.entity.AnalysisStatus;
import com.moa.moa_be.domain.analysis.entity.CategorySource;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

/**
 * 캡처 1장의 분석 결과. 실천 상세(GET /captures/{id}, C 파트)에서 이 값을 가져다 쓰면 됨.
 */
@Schema(description = "캡처 분석 결과")
public record CaptureAnalysisResponse(
        Long captureId,
        Long jobId,
        AnalysisStatus status,
        Category category,
        @Schema(example = "AI") CategorySource categorySource,
        LocalDateTime analyzedAt,
        @Schema(description = "SUCCEEDED 일 때만 값이 있음") AnalysisResult result
) {

    public record Category(Long id, String name) {
    }
}
