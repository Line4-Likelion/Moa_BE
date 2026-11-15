package com.moa.moa_be.domain.capture.dto;

import java.util.List;

/** 여러 장 폴더 이동 결과 */
public record CaptureItemsResponse(List<CaptureSummaryResponse> items) {
}
