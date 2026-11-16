
package com.moa.moa_be.domain.analysis.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;

/**
 * 검증까지 끝난 분석 결과.
 * Screenshot 에 저장하고 상세 화면에 내려줄 값.
 * box 는 [x, y, w, h] 0~1 비율, null 이면 앱에서 "원본에서 보기" 숨김.
 */
@Schema(description = "AI 분석 결과 (검증 완료)")
public record AnalysisResult(
        Deadline deadline,
        @Schema(example = "맛집") String category,
        @Schema(example = "0.86") double categoryConfidence,
        @Schema(example = "연남 소바실") String title,
        @Schema(example = "가을 한정 소바 행사") String summary,
        List<Fact> facts,
        @Schema(description = "실천 방법을 만들지 못하면 null → 앱에서 [다시 분석] 표시")
        Guide guide,
        List<String> checklist,
        @Schema(description = "이미지에서 추출한 장소 정보. 찾지 못하면 null")
        Place place
) {

    public enum DeadlineStatus {
        FOUND,
        UNCLEAR,
        NONE
    }

    public record Deadline(
            DeadlineStatus status,
            LocalDate date,
            String label,
            double[] box
    ) {
    }

    @Schema(description = "추출 정보. value 가 null 이면 앱에서 직접 확인 필요")
    public record Fact(
            String key,
            String label,
            String value,
            double[] box
    ) {
    }

    public record Guide(
            String title,
            List<String> lines,
            List<String> preparations,
            List<String> links
    ) {
    }

    @Schema(description = "이미지에서 추출한 장소 정보")
    public record Place(
            @Schema(example = "연남 소바실") String name,
            @Schema(example = "서울특별시 마포구 연남동 ...") String address
    ) {}
}
