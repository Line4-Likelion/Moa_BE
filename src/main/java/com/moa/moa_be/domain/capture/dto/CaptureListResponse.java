package com.moa.moa_be.domain.capture.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "캡처 목록")
public record CaptureListResponse(
        List<CaptureSummaryResponse> items,
        @JsonProperty("next_cursor") @Schema(description = "다음 페이지 cursor, 마지막이면 null") String nextCursor,
        @JsonInclude(JsonInclude.Include.NON_NULL)
        @Schema(description = "실천 목록·완료함에서만 내려감 (참고함은 없음)") Facets facets
) {

    @Schema(description = "현재 탭(status) 기준 개수")
    public record Facets(@Schema(example = "1") long snoozed, @Schema(example = "3") long total) {
    }
}
