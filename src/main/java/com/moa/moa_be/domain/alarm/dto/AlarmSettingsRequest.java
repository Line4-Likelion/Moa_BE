package com.moa.moa_be.domain.alarm.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class AlarmSettingsRequest {
    private int intervalHours;
    private boolean deadlineAlarmEnabled;
    private boolean dnd;
    private boolean locationAlarmEnabled;
}