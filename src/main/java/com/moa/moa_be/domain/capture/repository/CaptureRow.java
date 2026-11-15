package com.moa.moa_be.domain.capture.repository;

import com.moa.moa_be.domain.analysis.entity.ScreenshotAnalysis;
import com.moa.moa_be.domain.capture.entity.CaptureStatus;
import com.moa.moa_be.domain.screenshot.entity.ScreenshotClassification;

/** 캡처(A 파트 분류 기록) + 분석 결과(B 파트, 없으면 null) */
public record CaptureRow(ScreenshotClassification capture, ScreenshotAnalysis analysis) {

    public CaptureStatus status() {
        return CaptureStatus.of(capture, analysis);
    }
}
