package com.moa.moa_be.domain.capture.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.moa.moa_be.domain.analysis.dto.AnalysisResult.DeadlineStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;

/** 캡처 응답에 들어가는 하위 객체들 */
public final class CaptureParts {

    private CaptureParts() {
    }

    @Schema(description = "카테고리")
    public record CategoryInfo(
            @Schema(example = "3") Long id,
            @Schema(example = "맛집") String name,
            @JsonProperty("is_custom") boolean isCustom,
            @JsonProperty("todo_count") @Schema(description = "이 카테고리의 실천 대기 수") long todoCount
    ) {
    }

    @Schema(description = "마감기한. box 는 원본 이미지 속 위치 [x, y, w, h] 0~1 비율")
    public record DeadlineInfo(DeadlineStatus status, LocalDate date, String label, double[] box) {
    }

    @Schema(description = "체크리스트 진행도")
    public record Progress(@Schema(example = "2") int done, @Schema(example = "4") int total) {
    }

    @Schema(description = "핵심 정보. value 가 null 이면 AI가 찾지 못한 항목")
    public record FactItem(
            @Schema(example = "place") String key,
            @Schema(example = "장소") String label,
            @Schema(example = "서울 마포구 연남동") String value,
            @Schema(description = "[x, y, w, h] 0~1 비율, 없으면 null") double[] box,
            @Schema(description = "사용자가 고친 값이면 true") boolean edited
    ) {
    }

    public enum LinkType { WEB, MAP, PHONE }

    @Schema(description = "링크 (조회만)")
    public record LinkItem(LinkType type, @Schema(example = "naver.me") String label,
                           @Schema(example = "https://naver.me/abc") String url) {
    }

    @Schema(description = "실천 가이드")
    public record GuideInfo(String title, List<String> lines, List<String> preparations) {
    }

    @Schema(description = "체크리스트 항목")
    public record ChecklistItem(@Schema(example = "chk_1") String id, String text, boolean checked) {
    }

    @Schema(description = "분석 실패 사유")
    public record ErrorBody(@Schema(example = "ANALYSIS_FAILED") String code, String message) {
    }
}
