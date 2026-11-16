package com.moa.moa_be.domain.capture.entity;

/** POST /captures/{id}/actions 의 action */
public enum CaptureAction {
    COMPLETE, SNOOZE, DISMISS, UNDO, REOPEN
}
