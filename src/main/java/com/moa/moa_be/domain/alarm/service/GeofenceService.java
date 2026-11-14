package com.moa.moa_be.domain.alarm.service;

import com.moa.moa_be.domain.alarm.dto.GeofenceResponse;
import com.moa.moa_be.domain.alarm.dto.PlaceDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GeofenceService {

    // TODO: Capture 엔티티 및 리포지토리 구현 완료 후 연동 필요
    // private final CaptureRepository captureRepository;

    public GeofenceResponse getGeofencePlaces(Long userId) {
        // TODO: 추후 실제 DB 쿼리로 교체
        // List<Capture> captures = captureRepository.findByUserIdAndStatusAndLatIsNotNullAndLngIsNotNull(userId, CaptureStatus.TODO);

        List<PlaceDto> places = List.of();

        return GeofenceResponse.builder()
                .places(places)
                .build();
    }
}