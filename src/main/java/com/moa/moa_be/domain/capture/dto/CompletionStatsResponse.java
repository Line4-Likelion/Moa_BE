package com.moa.moa_be.domain.capture.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;

@Schema(description = "완료율 = 완료 ÷ (완료 + 실천 대기). 대상이 없으면 rate 는 null")
public record CompletionStatsResponse(
        @Schema(example = "0.17") Double rate,
        @JsonProperty("done_count") long doneCount,
        @JsonProperty("todo_count") long todoCount,
        @Schema(description = "최근 6주(월요일 시작). 대상이 없는 주는 빠짐") List<Weekly> weekly
) {

    public record Weekly(@JsonProperty("week_start") LocalDate weekStart, @Schema(example = "0.2") double rate) {
    }
}
