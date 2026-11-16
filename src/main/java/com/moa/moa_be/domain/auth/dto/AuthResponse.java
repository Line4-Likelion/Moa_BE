package com.moa.moa_be.domain.auth.dto;

import com.moa.moa_be.domain.user.dto.UserResponse;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AuthResponse {
    private String access_token;
    private String refresh_token;
    private long expires_in;
    private UserResponse user;
}