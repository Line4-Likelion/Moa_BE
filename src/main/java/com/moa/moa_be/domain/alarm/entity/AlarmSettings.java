package com.moa.moa_be.domain.alarm.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "alarm_settings")
public class AlarmSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long userId;

    private int intervalHours;
    private boolean deadlineAlarmEnabled;

    private boolean dnd;

    private boolean locationAlarmEnabled;

    @Builder
    public AlarmSettings(Long userId, int intervalHours, boolean deadlineAlarmEnabled, boolean dnd, boolean locationAlarmEnabled) {
        this.userId = userId;
        this.intervalHours = intervalHours;
        this.deadlineAlarmEnabled = deadlineAlarmEnabled;
        this.dnd = dnd;
        this.locationAlarmEnabled = locationAlarmEnabled;
    }

    public void update(int intervalHours, boolean deadlineAlarmEnabled, boolean dnd, boolean locationAlarmEnabled) {
        this.intervalHours = intervalHours;
        this.deadlineAlarmEnabled = deadlineAlarmEnabled;
        this.dnd = dnd;
        this.locationAlarmEnabled = locationAlarmEnabled;
    }
}