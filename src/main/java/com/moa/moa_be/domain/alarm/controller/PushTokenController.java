package com.moa.moa_be.domain.alarm.controller;

import com.moa.moa_be.domain.alarm.dto.PushTokenRequest;
import com.moa.moa_be.domain.alarm.service.PushTokenService;
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
@RequestMapping("/push-tokens")
@RequiredArgsConstructor
public class PushTokenController {

    private final PushTokenService pushTokenService;

    @Operation(summary = "푸시 토큰 등록", description = "기기의 FCM 푸시 토큰을 등록합니다.")
    @PostMapping
    public ResponseEntity<BaseResponse<Void>> registerPushToken(@RequestBody PushTokenRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new CustomException(GlobalErrorCode.UNAUTHORIZED);
        }

        Long userId = (Long) authentication.getPrincipal();
        pushTokenService.registerToken(userId, request);

        return ResponseEntity.ok(BaseResponse.success("푸시 토큰 등록에 성공했습니다.", null));
    }

    @Operation(summary = "푸시 토큰 삭제", description = "로그아웃 시 기기의 FCM 푸시 토큰을 삭제합니다.")
    @DeleteMapping("/{token}")
    public ResponseEntity<BaseResponse<Void>> deletePushToken(@PathVariable String token) {
        pushTokenService.deleteToken(token);

        return ResponseEntity.ok(BaseResponse.success("푸시 토큰 삭제에 성공했습니다.", null));
    }
}