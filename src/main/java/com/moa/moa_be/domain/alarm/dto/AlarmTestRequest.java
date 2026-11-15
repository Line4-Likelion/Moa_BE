package com.moa.moa_be.domain.alarm.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class AlarmTestRequest {
    private String type;
    private String captureId;
}