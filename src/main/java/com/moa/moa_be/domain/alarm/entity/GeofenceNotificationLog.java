package com.moa.moa_be.domain.alarm.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "geofence_notification_logs",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_geofence_user_capture_date",
                        columnNames = {"user_id", "capture_id", "notification_date"}
                )
        }
)
public class GeofenceNotificationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "capture_id", nullable = false)
    private Long captureId;

    @Column(name = "notification_date", nullable = false)
    private LocalDate notificationDate;
}