package com.moa.moa_be.domain.alarm.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PlaceDto {
    private String capture_id;
    private String title;
    private Double lat;
    private Double lng;
    private Integer radius_m;
}