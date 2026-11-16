package com.moa.moa_be.domain.alarm.repository;

import com.moa.moa_be.domain.alarm.entity.GeofenceNotificationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;

public interface GeofenceNotificationLogRepository
        extends JpaRepository<GeofenceNotificationLog, Long> {

    @Modifying
    @Query(value = """
            INSERT IGNORE INTO geofence_notification_logs
            (user_id, capture_id, notification_date)
            VALUES (:userId, :captureId, :notificationDate)
            """, nativeQuery = true)
    int insertIfAbsent(
            @Param("userId") Long userId,
            @Param("captureId") Long captureId,
            @Param("notificationDate") LocalDate notificationDate
    );

    @Modifying
    @Query("""
            DELETE FROM GeofenceNotificationLog log
            WHERE log.userId = :userId
              AND log.captureId = :captureId
              AND log.notificationDate = :notificationDate
            """)
    void deleteLog(
            @Param("userId") Long userId,
            @Param("captureId") Long captureId,
            @Param("notificationDate") LocalDate notificationDate
    );
}