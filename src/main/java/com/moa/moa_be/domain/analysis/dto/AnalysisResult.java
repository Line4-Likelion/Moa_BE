package com.moa.moa_be.domain.analysis.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;

/**
 * 검증까지 끝난 분석 결과. Screenshot 에 저장하고 상세 화면에 내려줄 값.
 * box 는 [x, y, w, h] 0~1 비율, null 이면 앱에서 "원본에서 보기" 숨김.
 */
@Schema(description = "AI 분석 결과 (검증 완료)")
@JsonIgnoreProperties(ignoreUnknown = true)
public record AnalysisResult(
        Deadline deadline,
        @Schema(example = "맛집") String category,
        @JsonProperty("category_confidence") @JsonAlias("categoryConfidence") // 예전 저장분 호환
        @Schema(example = "0.86") double categoryConfidence,
        @Schema(example = "연남 소바실") String title,
        @Schema(example = "가을 한정 소바 행사") String summary,
        List<Fact> facts,
        @Schema(description = "실천 방법을 만들지 못하면 null → 앱에서 [다시 분석] 표시") Guide guide,
        List<String> checklist
) {

    public enum DeadlineStatus {
        FOUND,   // 기한 있음
        UNCLEAR, // 날짜는 있지만 기한인지 불명확 → "마감기한 확인 필요"
        NONE     // 기한 없음
    }

    public record Deadline(DeadlineStatus status, LocalDate date, String label, double[] box) {
    }

    @Schema(description = "추출 정보. value 가 null 이면 앱에서 \"직접 확인 필요\"")
    public record Fact(String key, String label, String value, double[] box) {
    }

    public record Guide(String title, List<String> lines, List<String> preparations, List<String> links) {
    }
}
