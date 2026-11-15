package com.moa.moa_be.domain.alarm.service;

import com.moa.moa_be.domain.alarm.dto.AlarmSettingsRequest;
import com.moa.moa_be.domain.alarm.dto.AlarmSettingsResponse;
import com.moa.moa_be.domain.alarm.entity.AlarmSettings;
import com.moa.moa_be.domain.alarm.repository.AlarmSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AlarmSettingsService {

    private final AlarmSettingsRepository alarmSettingsRepository;

    @Transactional
    public AlarmSettingsResponse getSettings(Long userId) {
        AlarmSettings settings = alarmSettingsRepository.findByUserId(userId)
                .orElseGet(() -> createDefaultSettings(userId));
        return AlarmSettingsResponse.from(settings);
    }

    @Transactional
    public AlarmSettingsResponse updateSettings(Long userId, AlarmSettingsRequest request) {
        AlarmSettings settings = alarmSettingsRepository.findByUserId(userId)
                .orElseGet(() -> createDefaultSettings(userId));

        settings.update(
                request.getIntervalHours(),
                request.isDeadlineAlarmEnabled(),
                request.isDnd(),
                request.isLocationAlarmEnabled()
        );

        return AlarmSettingsResponse.from(settings);
    }

    @Transactional
    public AlarmSettings createDefaultSettings(Long userId) {
        AlarmSettings defaultSettings = AlarmSettings.builder()
                .userId(userId)
                .intervalHours(24)
                .deadlineAlarmEnabled(true)
                .dnd(false)
                .locationAlarmEnabled(false)
                .build();
        return alarmSettingsRepository.save(defaultSettings);
    }
}