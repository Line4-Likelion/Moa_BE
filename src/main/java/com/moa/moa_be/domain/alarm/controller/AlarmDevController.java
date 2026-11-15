package com.moa.moa_be.domain.alarm.controller;

import com.moa.moa_be.domain.alarm.dto.AlarmTestRequest;
import com.moa.moa_be.domain.alarm.service.FcmService;
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
@RequestMapping("/dev/alarms")
@RequiredArgsConstructor
public class AlarmDevController {

    private final FcmService fcmService;

    @Operation(summary = "개발/시연용 테스트 알림 전송", description = "지정한 알림을 내 기기로 바로 전송합니다. (개발 서버 전용)")
    @PostMapping("/test")
    public ResponseEntity<BaseResponse<Void>> sendTestAlarm(
            @RequestBody AlarmTestRequest request
    ) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new CustomException(GlobalErrorCode.UNAUTHORIZED);
        }

        Long userId = (Long) authentication.getPrincipal();

        fcmService.sendTestNotificationByType(userId, request.getType(), request.getCaptureId());

        return ResponseEntity.ok(BaseResponse.success("테스트 알림이 성공적으로 전송되었습니다.", null));
    }
}