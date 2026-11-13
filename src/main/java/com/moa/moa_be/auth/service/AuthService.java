package com.moa.moa_be.auth.service;

import com.moa.moa_be.auth.dto.AuthResponse;
import com.moa.moa_be.auth.dto.LoginRequest;
import com.moa.moa_be.auth.jwt.JwtTokenProvider;
import com.moa.moa_be.global.exception.CustomException;
import com.moa.moa_be.global.exception.GlobalErrorCode;
import com.moa.moa_be.user.dto.SignUpRequest;
import com.moa.moa_be.user.dto.UserResponse;
import com.moa.moa_be.user.entity.User;
import com.moa.moa_be.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    // 회원가입
    @Transactional
    public AuthResponse signup(SignUpRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new CustomException(GlobalErrorCode.EMAIL_ALREADY_EXISTS);
        }

        User user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .nickname(request.getNickname())
                .build();

        User savedUser = userRepository.save(user);

        String accessToken = jwtTokenProvider.createAccessToken(savedUser.getId(), savedUser.getEmail(), "USER");
        String refreshToken = jwtTokenProvider.createRefreshToken(savedUser.getId(), savedUser.getEmail(), "USER");

        return new AuthResponse(accessToken, refreshToken, 3600, new UserResponse(savedUser));
    }

    // 로그인
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new CustomException(GlobalErrorCode.INVALID_CREDENTIALS));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new CustomException(GlobalErrorCode.INVALID_CREDENTIALS);
        }

        String accessToken = jwtTokenProvider.createAccessToken(user.getId(), user.getEmail(), "USER");
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId(), user.getEmail(), "USER");

        return new AuthResponse(accessToken, refreshToken, 3600, new UserResponse(user));
    }
}