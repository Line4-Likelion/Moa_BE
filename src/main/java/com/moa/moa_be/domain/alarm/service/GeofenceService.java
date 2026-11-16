package com.moa.moa_be.domain.alarm.service;

import com.moa.moa_be.domain.alarm.dto.GeofenceArrivalResponse;
import com.moa.moa_be.domain.alarm.dto.GeofenceResponse;
import com.moa.moa_be.domain.alarm.dto.PlaceDto;
import com.moa.moa_be.domain.alarm.entity.AlarmSettings;
import com.moa.moa_be.domain.alarm.repository.AlarmSettingsRepository;
import com.moa.moa_be.domain.alarm.repository.GeofenceNotificationLogRepository;
import com.moa.moa_be.domain.analysis.entity.AnalysisStatus;
import com.moa.moa_be.domain.analysis.entity.ScreenshotAnalysis;
import com.moa.moa_be.domain.analysis.repository.ScreenshotAnalysisRepository;
import com.moa.moa_be.global.exception.CustomException;
import com.moa.moa_be.global.exception.GlobalErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GeofenceService {

    private static final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");

    private final ScreenshotAnalysisRepository screenshotAnalysisRepository;
    private final AlarmSettingsRepository alarmSettingsRepository;
    private final GeofenceNotificationLogRepository notificationLogRepository;
    private final FcmService fcmService;

    public GeofenceResponse getGeofencePlaces(Long userId) {

        boolean locationAlarmEnabled = alarmSettingsRepository.findByUserId(userId)
                .map(AlarmSettings::isLocationAlarmEnabled)
                .orElse(false);

        if (!locationAlarmEnabled) {
            return GeofenceResponse.builder()
                    .places(List.of())
                    .build();
        }

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

    @Transactional
    public GeofenceArrivalResponse notifyArrival(Long userId, String captureIdValue) {

        Long captureId = parseCaptureId(captureIdValue);

        ScreenshotAnalysis capture = screenshotAnalysisRepository
                .findByCaptureIdAndUserId(captureId, userId)
                .orElseThrow(() -> new CustomException(GlobalErrorCode.NOT_FOUND));

        if (capture.getStatus() != AnalysisStatus.SUCCEEDED
                || capture.getLatitude() == null
                || capture.getLongitude() == null) {
            throw new CustomException(GlobalErrorCode.NOT_FOUND);
        }

        AlarmSettings settings = alarmSettingsRepository.findByUserId(userId)
                .orElse(null);

        if (settings == null || !settings.isLocationAlarmEnabled()) {
            return GeofenceArrivalResponse.builder()
                    .notified(false)
                    .message("위치 알림이 꺼져 있습니다.")
                    .build();
        }

        String placeName = capture.getPlaceName() != null
                ? capture.getPlaceName()
                : capture.getTitle();

        if (placeName == null || placeName.isBlank()) {
            placeName = "저장한 장소";
        }

        LocalDate today = LocalDate.now(KOREA_ZONE);

        int inserted = notificationLogRepository.insertIfAbsent(
                userId,
                captureId,
                today
        );

        if (inserted == 0) {
            return GeofenceArrivalResponse.builder()
                    .notified(false)
                    .message("오늘 이미 알림을 보낸 장소입니다.")
                    .build();
        }

        boolean sent = fcmService.sendLocationArrival(
                userId,
                captureId,
                placeName
        );

        if (!sent) {
            notificationLogRepository.deleteLog(userId, captureId, today);

            return GeofenceArrivalResponse.builder()
                    .notified(false)
                    .message("알림 전송에 실패했습니다.")
                    .build();
        }

        return GeofenceArrivalResponse.builder()
                .notified(true)
                .message("장소 도착 알림을 전송했습니다.")
                .build();
    }

    private Long parseCaptureId(String captureIdValue) {

        if (captureIdValue == null || !captureIdValue.startsWith("cap_")) {
            throw new CustomException(GlobalErrorCode.VALIDATION_ERROR);
        }

        try {
            return Long.parseLong(captureIdValue.substring(4));
        } catch (NumberFormatException e) {
            throw new CustomException(GlobalErrorCode.VALIDATION_ERROR);
        }
    }
}