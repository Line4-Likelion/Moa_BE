package com.moa.moa_be.domain.alarm.controller;

import com.moa.moa_be.domain.alarm.dto.AlarmSettingsRequest;
import com.moa.moa_be.domain.alarm.dto.AlarmSettingsResponse;
import com.moa.moa_be.domain.alarm.service.AlarmSettingsService;
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

@Tag(name = "Alarm & Push Token", description = "알림 및 푸시 토큰 관리 API")
@RestController
@RequestMapping("/settings/alarm")
@RequiredArgsConstructor
public class AlarmSettingsController {

    private final AlarmSettingsService alarmSettingsService;

    @Operation(summary = "알림 설정 조회", description = "로그인한 유저의 알림 설정 정보를 조회합니다.")
    @GetMapping
    public ResponseEntity<BaseResponse<AlarmSettingsResponse>> getAlarmSettings() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new CustomException(GlobalErrorCode.UNAUTHORIZED);
        }

        Long userId = (Long) authentication.getPrincipal();
        AlarmSettingsResponse response = alarmSettingsService.getSettings(userId);

        return ResponseEntity.ok(BaseResponse.success("알림 설정 조회에 성공했습니다.", response));
    }

    @Operation(summary = "알림 설정 수정", description = "로그인한 유저의 알림 설정 정보를 수정합니다.")
    @PutMapping
    public ResponseEntity<BaseResponse<AlarmSettingsResponse>> updateAlarmSettings(
            @RequestBody AlarmSettingsRequest request
    ) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new CustomException(GlobalErrorCode.UNAUTHORIZED);
        }

        Long userId = (Long) authentication.getPrincipal();
        AlarmSettingsResponse response = alarmSettingsService.updateSettings(userId, request);

        return ResponseEntity.ok(BaseResponse.success("알림 설정 수정에 성공했습니다.", response));
    }
}