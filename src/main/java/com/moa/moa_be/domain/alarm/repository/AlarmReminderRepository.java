
package com.moa.moa_be.domain.alarm.repository;

import com.moa.moa_be.domain.alarm.entity.AlarmReminder;
import com.moa.moa_be.domain.alarm.entity.AlarmReminder.ReminderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AlarmReminderRepository extends JpaRepository<AlarmReminder, Long> {

    Optional<AlarmReminder> findByCaptureId(Long captureId);

    List<AlarmReminder> findAllByStatusAndAlarmTimeLessThanEqual(
            ReminderStatus status,
            LocalDateTime alarmTime
    );

    void deleteByCaptureId(Long captureId);
}
