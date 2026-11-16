package com.moa.moa_be.domain.alarm.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AlarmTestRequest {
    private String type;
    private String captureId;
    private String title;
    private String body;
}