package com.moa.moa_be.domain.capture.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "바꾼 항목만 전송. value 가 null 이면 AI가 읽은 값으로 되돌림")
public record FactsUpdateRequest(List<FactPatch> facts) {

    public record FactPatch(@Schema(example = "deadline") String key,
                            @Schema(example = "2026-11-30") String value) {
    }
}
