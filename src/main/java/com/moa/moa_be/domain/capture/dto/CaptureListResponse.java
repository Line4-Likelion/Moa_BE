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
        @Schema(description = "현재 탭(status) 기준 개수. 카테고리 필터와 무관") Facets facets
) {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "실천 목록·완료함: snoozed, total / 참고함: total, categories")
    public record Facets(
            @Schema(description = "보류 중 개수 (실천 목록·완료함만)", example = "1") Long snoozed,
            @Schema(example = "3") long total,
            @Schema(description = "카테고리별 개수 (참고함만). 카테고리 없는 항목은 기타에 포함") List<CategoryCount> categories
    ) {

        public static Facets ofTab(long snoozed, long total) {
            return new Facets(snoozed, total, null);
        }

        public static Facets ofReference(List<CategoryCount> categories) {
            return new Facets(null, categories.stream().mapToLong(CategoryCount::count).sum(), categories);
        }
    }

    public record CategoryCount(@Schema(example = "3") Long id, @Schema(example = "맛집") String name,
                                @Schema(example = "1") long count) {
    }
}
