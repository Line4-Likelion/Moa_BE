package com.moa.moa_be.domain.alarm.service;

import com.moa.moa_be.domain.alarm.entity.PushToken;
import com.moa.moa_be.domain.alarm.repository.PushTokenRepository;
import com.moa.moa_be.domain.alarm.dto.PushTokenRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PushTokenService {

    private final PushTokenRepository pushTokenRepository;

    @Transactional
    public void registerToken(Long userId, PushTokenRequest request) {
        pushTokenRepository.findByToken(request.getToken())
                .ifPresentOrElse(
                        existingToken -> {
                            existingToken.updateUser(userId);
                        },
                        () -> {
                            PushToken pushToken = PushToken.builder()
                                    .userId(userId)
                                    .token(request.getToken())
                                    .platform(request.getPlatform())
                                    .build();
                            pushTokenRepository.save(pushToken);
                        }
                );
    }

    @Transactional
    public void deleteToken(String token) {
        pushTokenRepository.deleteByToken(token);
    }
}