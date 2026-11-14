package com.moa.moa_be.domain.screenshot.dto;

import com.moa.moa_be.domain.screenshot.entity.Classification;
import com.moa.moa_be.domain.screenshot.entity.ScreenshotClassification;
import java.time.Instant;

public record ClassificationResponse(Long id, String fileKey, Classification classification, Instant classifiedAt) {
    public static ClassificationResponse from(ScreenshotClassification entity) {
        return new ClassificationResponse(entity.getId(), entity.getFileKey(), entity.getClassification(), entity.getClassifiedAt());
    }
}
