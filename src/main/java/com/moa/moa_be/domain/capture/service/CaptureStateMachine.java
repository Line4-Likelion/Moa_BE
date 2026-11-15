package com.moa.moa_be.domain.capture.service;

import com.moa.moa_be.domain.capture.entity.CaptureAction;
import com.moa.moa_be.domain.capture.entity.CaptureStatus;
import com.moa.moa_be.global.exception.CustomException;
import com.moa.moa_be.global.exception.GlobalErrorCode;

/**
 * 상태 전이 규칙 (POST /captures/{id}/actions).
 * <pre>
 * COMPLETE  TODO → DONE
 * SNOOZE    TODO → TODO + snooze_until
 * DISMISS   TODO → REFERENCE
 * REOPEN    DONE → TODO   (이미 TODO 면 변경 없음)
 * UNDO      상태와 관계없이 되돌리기 토큰으로 판단
 * 그 외      409 INVALID_STATE_TRANSITION
 * </pre>
 */
public final class CaptureStateMachine {

    public enum Outcome {
        /** 동작 실행 */
        APPLY,
        /** 변경 없이 200 */
        NO_CHANGE
    }

    private CaptureStateMachine() {
    }

    public static Outcome decide(CaptureStatus current, CaptureAction action) {
        return switch (action) {
            case COMPLETE, SNOOZE, DISMISS -> require(current == CaptureStatus.TODO);
            case REOPEN -> current == CaptureStatus.TODO ? Outcome.NO_CHANGE : require(current == CaptureStatus.DONE);
            case UNDO -> Outcome.APPLY;
        };
    }

    private static Outcome require(boolean allowed) {
        if (!allowed) {
            throw new CustomException(GlobalErrorCode.INVALID_STATE_TRANSITION);
        }
        return Outcome.APPLY;
    }
}
