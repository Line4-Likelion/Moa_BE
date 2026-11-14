package com.moa.moa_be.domain.alarm.dto;

import com.moa.moa_be.domain.alarm.entity.AlarmSettings;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AlarmSettingsResponse {
    private int intervalHours;
    private boolean deadlineAlarmEnabled;
    private boolean dnd;
    private boolean locationAlarmEnabled;

    public static AlarmSettingsResponse from(AlarmSettings settings) {
        return AlarmSettingsResponse.builder()
                .intervalHours(settings.getIntervalHours())
                .deadlineAlarmEnabled(settings.isDeadlineAlarmEnabled())
                .dnd(settings.isDnd())
                .locationAlarmEnabled(settings.isLocationAlarmEnabled())
                .build();
    }
}