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
 */
public record CaptureListCondition(
        CaptureStatus status,
        Boolean snoozed,
        LocalDate today,
        Instant completedFrom,
        Instant completedTo,
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
        return new CaptureListCondition(status, null, today, null, null, sort);
    }

    public CaptureListCondition withSnoozed(Boolean snoozed) {
        return new CaptureListCondition(status, snoozed, today, completedFrom, completedTo, sort);
    }
}
