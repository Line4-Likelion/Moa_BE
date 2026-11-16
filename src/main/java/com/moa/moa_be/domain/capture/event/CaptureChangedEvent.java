package com.moa.moa_be.domain.capture.event;

import com.moa.moa_be.domain.capture.entity.CaptureStatus;
import java.time.LocalDate;

/**
 * 캡처의 상태(status) 또는 마감기한(deadline)이 바뀌었을 때 발행.
 * 알림 모듈이 구독해 알림 예약·취소를 처리한다. 트랜잭션 안에서 발행하므로
 * 구독 쪽은 {@code @TransactionalEventListener}(AFTER_COMMIT)로 받는 것을 권장.
 *
 * @param currentStatus 캡처가 삭제됐으면 null
 */
public record CaptureChangedEvent(
        Long userId,
        Long captureId,
        CaptureStatus previousStatus,
        CaptureStatus currentStatus,
        LocalDate previousDeadline,
        LocalDate currentDeadline
) {
}
