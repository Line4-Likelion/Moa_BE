package com.moa.moa_be.domain.alarm.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CoordinateDto {
    private double lat; // 위도
    private double lng; // 경도
}