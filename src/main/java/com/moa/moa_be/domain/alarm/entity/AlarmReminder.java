
package com.moa.moa_be.domain.alarm.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "alarm_reminders",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_alarm_reminder_capture",
                        columnNames = "capture_id"
                )
        },
        indexes = {
                @Index(name = "idx_alarm_reminder_due", columnList = "status, alarm_time")
        }
)
public class AlarmReminder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "capture_id", nullable = false)
    private Long captureId;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(name = "deadline_date", nullable = false)
    private LocalDate deadlineDate;

    @Column(name = "alarm_time", nullable = false)
    private LocalDateTime alarmTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReminderStatus status;

    @Column(name = "retry_count", nullable = false)
    private int retryCount = 0;

    public static AlarmReminder create() {
        return new AlarmReminder();
    }

    public void schedule(
            Long userId,
            Long captureId,
            String title,
            LocalDate deadlineDate,
            LocalDateTime alarmTime
    ) {
        this.userId = userId;
        this.captureId = captureId;
        this.title = title;
        this.deadlineDate = deadlineDate;
        this.alarmTime = alarmTime;
        this.status = ReminderStatus.PENDING;
    }

    public void markSent() {
        this.status = ReminderStatus.SENT;
    }

    public void markFailed() {
        this.status = ReminderStatus.FAILED;
    }

    public void cancel() {
        this.status = ReminderStatus.CANCELLED;
    }

    public void increaseRetryCount() {
        this.retryCount++;
    }

    public enum ReminderStatus {
        PENDING,
        SENT,
        CANCELLED,
        FAILED
    }
}
