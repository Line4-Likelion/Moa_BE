package com.moa.moa_be.domain.capture.repository;

import com.moa.moa_be.domain.capture.entity.CaptureStatus;
import java.time.Instant;
import java.time.LocalDate;

/**
 * GET /captures 조회 조건.
 *
 * @param snoozed       null 이면 전체, true 면 보류 중만, false 면 보류 아닌 것만
 * @param completedFrom completed_on 필터 시작 (포함), null 이면 필터 없음
 * @param completedTo   completed_on 필터 끝 (미포함)
 * @param categoryId    카테고리 필터. 실천(TODO·DONE)은 분석 결과의 카테고리, 참고함은 캡처의 카테고리
 * @param categoryIsOther 필터 카테고리가 "기타"면 true → 참고함에서 카테고리 없는 항목도 포함
 */
public record CaptureListCondition(
        CaptureStatus status,
        Boolean snoozed,
        LocalDate today,
        Instant completedFrom,
        Instant completedTo,
        Long categoryId,
        boolean categoryIsOther,
        Sort sort
) {

    public enum Sort {
        /** 가까운 마감순, 마감 없으면 뒤에 최근순 */
        DEADLINE,
        /** 상태가 바뀐(저장·완료된) 최신순 */
        RECENT
    }

    /** facets 집계용: 탭(status)만 남기고 다른 필터는 뺀 조건 */
    public CaptureListCondition tabOnly() {
        return new CaptureListCondition(status, null, today, null, null, null, false, sort);
    }

    public CaptureListCondition withSnoozed(Boolean snoozed) {
        return new CaptureListCondition(status, snoozed, today, completedFrom, completedTo, categoryId,
                categoryIsOther, sort);
    }
}
