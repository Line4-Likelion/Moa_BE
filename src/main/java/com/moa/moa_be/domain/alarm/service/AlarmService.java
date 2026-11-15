package com.moa.moa_be.domain.alarm.service;

import com.moa.moa_be.domain.alarm.entity.AlarmReminder;
import com.moa.moa_be.domain.alarm.entity.AlarmReminder.ReminderStatus;
import com.moa.moa_be.domain.alarm.entity.AlarmSettings;
import com.moa.moa_be.domain.alarm.repository.AlarmReminderRepository;
import com.moa.moa_be.domain.alarm.repository.AlarmSettingsRepository;
import com.moa.moa_be.domain.analysis.dto.AnalysisResult.DeadlineStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlarmService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final FcmService fcmService;
    private final AlarmReminderRepository alarmReminderRepository;
    private final AlarmSettingsRepository alarmSettingsRepository;

    @Transactional
    public void scheduleDeadlineAlarm(
            Long userId,
            Long captureId,
            String title,
            DeadlineStatus deadlineStatus,
            LocalDate deadlineDate
    ) {
        alarmReminderRepository.deleteByCaptureId(captureId);

        if (deadlineStatus != DeadlineStatus.FOUND || deadlineDate == null) {
            log.info("마감 알림 예약 제외 userId={}, captureId={}, status={}",
                    userId, captureId, deadlineStatus);
            return;
        }

        if (deadlineDate.isBefore(LocalDate.now(KST))) {
            log.info("지난 마감일이므로 알림 예약 제외 userId={}, captureId={}, deadlineDate={}",
                    userId, captureId, deadlineDate);
            return;
        }

        LocalDateTime alarmTime = deadlineDate.atTime(9, 0);

        AlarmReminder reminder = AlarmReminder.create();
        reminder.schedule(
                userId,
                captureId,
                title == null || title.isBlank() ? "마감 항목" : title,
                deadlineDate,
                alarmTime
        );

        alarmReminderRepository.save(reminder);

        log.info("마감 알림 예약 완료 userId={}, captureId={}, alarmTime={}",
                userId, captureId, alarmTime);
    }

    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void sendDueDeadlineAlarms() {
        LocalDateTime now = LocalDateTime.now(KST);

        List<AlarmReminder> reminders =
                alarmReminderRepository.findAllByStatusAndAlarmTimeLessThanEqual(
                        ReminderStatus.PENDING,
                        now
                );

        for (AlarmReminder reminder : reminders) {
            try {
                AlarmSettings settings = alarmSettingsRepository
                        .findByUserId(reminder.getUserId())
                        .orElse(null);

                if (settings != null && settings.isDnd()) {
                    reminder.cancel();
                    log.info("방해금지 설정으로 마감 알림 취소 userId={}, captureId={}",
                            reminder.getUserId(), reminder.getCaptureId());
                    continue;
                }

                if (settings != null && !settings.isDeadlineAlarmEnabled()) {
                    reminder.cancel();
                    log.info("마감 알림 설정 OFF로 예약 취소 userId={}, captureId={}",
                            reminder.getUserId(), reminder.getCaptureId());
                    continue;
                }

                boolean sent = fcmService.sendDeadlineReminder(
                        reminder.getUserId(),
                        reminder.getCaptureId(),
                        reminder.getTitle(),
                        reminder.getDeadlineDate()
                );

                if (sent) {
                    reminder.markSent();
                }

            } catch (Exception e) {
                log.error("마감 알림 처리 실패 captureId={}: {}",
                        reminder.getCaptureId(), e.getMessage(), e);
            }
        }
    }
}