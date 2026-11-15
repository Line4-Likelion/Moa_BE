package com.moa.moa_be.domain.alarm.repository;

import com.moa.moa_be.domain.alarm.entity.PushToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PushTokenRepository extends JpaRepository<PushToken, Long> {
    Optional<PushToken> findByToken(String token);
    void deleteByToken(String token);
    List<PushToken> findAllByUserId(Long userId);
}