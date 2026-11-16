
package com.moa.moa_be.domain.alarm.service;

import com.moa.moa_be.domain.alarm.dto.GeofenceResponse;
import com.moa.moa_be.domain.alarm.dto.PlaceDto;
import com.moa.moa_be.domain.analysis.entity.AnalysisStatus;
import com.moa.moa_be.domain.analysis.entity.ScreenshotAnalysis;
import com.moa.moa_be.domain.analysis.repository.ScreenshotAnalysisRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GeofenceService {

    private final ScreenshotAnalysisRepository screenshotAnalysisRepository;

    public GeofenceResponse getGeofencePlaces(Long userId) {
        List<ScreenshotAnalysis> captures =
                screenshotAnalysisRepository
                        .findAllByUserIdAndStatusAndLatitudeIsNotNullAndLongitudeIsNotNull(
                                userId,
                                AnalysisStatus.SUCCEEDED
                        );

        List<PlaceDto> places = captures.stream()
                .map(capture -> PlaceDto.builder()
                        .capture_id("cap_" + capture.getCaptureId())
                        .title(capture.getPlaceName() != null
                                ? capture.getPlaceName()
                                : capture.getTitle())
                        .lat(capture.getLatitude())
                        .lng(capture.getLongitude())
                        .radius_m(300)
                        .build())
                .collect(Collectors.toList());

        return GeofenceResponse.builder()
                .places(places)
                .build();
    }
}
