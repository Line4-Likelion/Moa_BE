package com.moa.moa_be.domain.capture.entity;

import com.moa.moa_be.domain.analysis.entity.AnalysisStatus;
import com.moa.moa_be.domain.analysis.entity.ScreenshotAnalysis;
import com.moa.moa_be.domain.screenshot.entity.Classification;
import com.moa.moa_be.domain.screenshot.entity.ScreenshotClassification;

/**
 * 캡처 상태. DB에 따로 저장하지 않고 분류(A)·분석(B)·완료 여부로 계산한다.
 * 보류는 별도 상태가 아니라 TODO + snooze_until.
 */
public enum CaptureStatus {
    ANALYZING, FAILED, TODO, DONE, REFERENCE;

    /** @return 삭제 기록(DELETE 분류)이면 null */
    public static CaptureStatus of(ScreenshotClassification capture, ScreenshotAnalysis analysis) {
        if (capture.getClassification() == Classification.REFERENCE) {
            return REFERENCE;
        }
        if (capture.getClassification() != Classification.PRACTICE) {
            return null;
        }
        if (analysis == null || analysis.getStatus().isInProgress()) {
            return ANALYZING;
        }
        if (analysis.getStatus() == AnalysisStatus.FAILED) {
            return FAILED;
        }
        return capture.getCompletedAt() == null ? TODO : DONE;
    }
}
