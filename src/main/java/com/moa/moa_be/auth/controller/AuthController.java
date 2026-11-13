package com.moa.moa_be.auth.controller;

import com.moa.moa_be.auth.dto.AuthResponse;
import com.moa.moa_be.auth.dto.LoginRequest;
import com.moa.moa_be.auth.service.AuthService;
import com.moa.moa_be.global.common.BaseResponse;
import com.moa.moa_be.user.dto.SignUpRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Auth", description = "사용자 인증 및 회원가입/로그인 API")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "회원가입", description = "이메일, 비밀번호, 닉네임으로 회원가입을 진행하고 토큰과 유저 정보를 반환합니다.")
    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public BaseResponse<AuthResponse> signup(@RequestBody @Valid SignUpRequest request) {
        AuthResponse response = authService.signup(request);
        return BaseResponse.success(response);
    }

    @Operation(summary = "로그인", description = "이메일과 비밀번호로 로그인하고 토큰과 유저 정보를 반환합니다.")
    @PostMapping("/login")
    public BaseResponse<AuthResponse> login(@RequestBody @Valid LoginRequest request) {
        AuthResponse response = authService.login(request);
        return BaseResponse.success(response);
    }
}