package com.moa.moa_be.domain.capture.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.moa.moa_be.domain.capture.entity.CaptureAction;
import com.moa.moa_be.domain.capture.entity.CaptureStatus;
import com.moa.moa_be.domain.capture.service.CaptureStateMachine.Outcome;
import com.moa.moa_be.global.exception.CustomException;
import com.moa.moa_be.global.exception.GlobalErrorCode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class CaptureStateMachineTest {

    @ParameterizedTest(name = "{0} + {1} → {2}")
    @CsvSource({
            "TODO, COMPLETE, APPLY",
            "TODO, SNOOZE, APPLY",
            "TODO, DISMISS, APPLY",
            "DONE, REOPEN, APPLY",
            "TODO, REOPEN, NO_CHANGE",
            "TODO, UNDO, APPLY",
            "DONE, UNDO, APPLY",
            "REFERENCE, UNDO, APPLY",
    })
    void allowed(CaptureStatus status, CaptureAction action, Outcome expected) {
        assertEquals(expected, CaptureStateMachine.decide(status, action));
    }

    @ParameterizedTest(name = "{0} + {1} → 409")
    @CsvSource({
            "ANALYZING, COMPLETE", "ANALYZING, SNOOZE", "ANALYZING, DISMISS", "ANALYZING, REOPEN",
            "FAILED, COMPLETE", "FAILED, SNOOZE", "FAILED, DISMISS", "FAILED, REOPEN",
            "DONE, COMPLETE", "DONE, SNOOZE", "DONE, DISMISS",
            "REFERENCE, COMPLETE", "REFERENCE, SNOOZE", "REFERENCE, DISMISS", "REFERENCE, REOPEN",
    })
    void rejected(CaptureStatus status, CaptureAction action) {
        CustomException e = assertThrows(CustomException.class, () -> CaptureStateMachine.decide(status, action));
        assertEquals(GlobalErrorCode.INVALID_STATE_TRANSITION, e.getErrorCode());
    }
}
