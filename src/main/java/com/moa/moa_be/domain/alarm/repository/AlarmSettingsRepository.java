package com.moa.moa_be.domain.alarm.repository;

import com.moa.moa_be.domain.alarm.entity.AlarmSettings;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AlarmSettingsRepository extends JpaRepository<AlarmSettings, Long> {
    Optional<AlarmSettings> findByUserId(Long userId);
}