package com.moa.moa_be.domain.alarm.controller;

import com.moa.moa_be.domain.alarm.dto.GeofenceArrivalResponse;
import com.moa.moa_be.domain.alarm.dto.GeofenceResponse;
import com.moa.moa_be.domain.alarm.service.GeofenceService;
import com.moa.moa_be.global.common.BaseResponse;
import com.moa.moa_be.global.exception.CustomException;
import com.moa.moa_be.global.exception.GlobalErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Alarm & Geofence", description = "위치 기반 알림 및 지오펜스 관리 API")
@RestController
@RequiredArgsConstructor
public class GeofenceController {

    private final GeofenceService geofenceService;

    @Operation(summary = "위치 기반 알림 장소 목록 조회", description = "위치 알림이 켜진 경우 좌표가 존재하는 장소 목록을 조회합니다.")
    @GetMapping("/geofences")
    public ResponseEntity<BaseResponse<GeofenceResponse>> getGeofences() {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || authentication.getPrincipal() == null) {
            throw new CustomException(GlobalErrorCode.UNAUTHORIZED);
        }

        Long userId = (Long) authentication.getPrincipal();

        GeofenceResponse response = geofenceService.getGeofencePlaces(userId);

        return ResponseEntity.ok(
                BaseResponse.success("위치 기반 알림 장소 목록 조회에 성공했습니다.", response)
        );
    }

    @Operation(summary = "장소 도착 알림", description = "지오펜스 진입 시 호출하며, 장소별 하루 한 번만 알림을 전송합니다.")
    @PostMapping("/geofences/{captureId}/arrived")
    public ResponseEntity<BaseResponse<GeofenceArrivalResponse>> notifyArrival(@PathVariable String captureId) {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || authentication.getPrincipal() == null) {
            throw new CustomException(GlobalErrorCode.UNAUTHORIZED);
        }

        Long userId = (Long) authentication.getPrincipal();

        GeofenceArrivalResponse response =
                geofenceService.notifyArrival(userId, captureId);

        return ResponseEntity.ok(
                BaseResponse.success(response.getMessage(), response)
        );
    }
}