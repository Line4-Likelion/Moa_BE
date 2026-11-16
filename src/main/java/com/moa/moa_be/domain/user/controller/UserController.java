package com.moa.moa_be.domain.user.controller;

import com.moa.moa_be.global.common.BaseResponse;
import com.moa.moa_be.global.exception.CustomException;
import com.moa.moa_be.global.exception.GlobalErrorCode;
import com.moa.moa_be.domain.user.dto.UserResponse;
import com.moa.moa_be.domain.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@Tag(name = "User", description = "사용자 정보 API")
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "내 프로필 조회", description = "로그인한 유저의 정보를 조회합니다.")
    @GetMapping("/me")
    public ResponseEntity<BaseResponse<UserResponse>> getMyInfo() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new CustomException(GlobalErrorCode.UNAUTHORIZED);
        }

        Long userId = (Long) authentication.getPrincipal();
        UserResponse response = userService.getMyInfo(userId);

        return ResponseEntity.ok(BaseResponse.success("내 정보 조회에 성공했습니다.", response));
    }

    @Operation(summary = "회원 탈퇴", description = "로그인한 유저의 계정을 삭제합니다.")
    @DeleteMapping("/me")
    public ResponseEntity<BaseResponse<Void>> withdrawUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new CustomException(GlobalErrorCode.UNAUTHORIZED);
        }

        Long userId = (Long) authentication.getPrincipal();
        userService.withdrawUser(userId);

        return ResponseEntity.ok(BaseResponse.success("회원 탈퇴에 성공했습니다.", null));
    }
}