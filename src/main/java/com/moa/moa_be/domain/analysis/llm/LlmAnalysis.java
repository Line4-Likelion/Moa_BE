package com.moa.moa_be.domain.analysis.llm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * LLM 이 돌려준 원본 JSON (API 명세서 ③ LLM 응답). 아직 검증 전이라 그대로 믿으면 안 됨.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record LlmAnalysis(
        Deadline deadline,
        String category,
        @JsonProperty("category_confidence") Double categoryConfidence,
        String title,
        String summary,
        List<Fact> facts,
        Guide guide,
        List<String> checklist,
        Place place
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Deadline(
            String status,
            String date,
            String label,
            @JsonProperty("source_block_ids") List<String> sourceBlockIds
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Fact(
            String key,
            String label,
            String value,
            @JsonProperty("source_block_ids") List<String> sourceBlockIds
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Guide(
            String title,
            List<String> lines,
            List<String> preparations,
            List<String> links
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Place(
            String name,
            String address
    ) {
    }
}
