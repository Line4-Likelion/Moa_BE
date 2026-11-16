package com.moa.moa_be.domain.analysis.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.moa.moa_be.domain.analysis.entity.AnalysisStatus;
import com.moa.moa_be.domain.analysis.entity.AnalysisStep;
import com.moa.moa_be.domain.analysis.entity.ScreenshotAnalysis;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "분석 작업 상태 목록 (2초 간격 폴링)")
public record AnalysisJobStatusResponse(List<Job> jobs) {

    public record Job(
            @JsonProperty("job_id") Long jobId,
            @JsonProperty("capture_id") Long captureId,
            @Schema(example = "RUNNING") AnalysisStatus status,
            @Schema(example = "OCR") AnalysisStep step,
            @Schema(description = "FAILED 일 때만 값이 있음") Error error
    ) {
        public static Job from(ScreenshotAnalysis a) {
            Error error = a.getStatus() == AnalysisStatus.FAILED
                    ? new Error(a.getErrorCode(), a.getErrorMessage())
                    : null;
            return new Job(a.getId(), a.getCaptureId(), a.getStatus(), a.getStep(), error);
        }
    }

    public record Error(
            @Schema(example = "ANALYSIS_TIMEOUT") String code,
            @Schema(example = "AI 분석 작업 시간이 초과되었습니다.") String message
    ) {
    }
}
