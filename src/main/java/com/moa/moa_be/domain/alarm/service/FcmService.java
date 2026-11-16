
package com.moa.moa_be.domain.alarm.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.moa.moa_be.domain.alarm.entity.AlarmSettings;
import com.moa.moa_be.domain.alarm.entity.PushToken;
import com.moa.moa_be.domain.alarm.repository.AlarmSettingsRepository;
import com.moa.moa_be.domain.alarm.repository.PushTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FcmService {

    private final PushTokenRepository pushTokenRepository;
    private final AlarmSettingsRepository alarmSettingsRepository;

    private boolean isDndEnabled(Long userId) {
        return alarmSettingsRepository.findByUserId(userId)
                .map(AlarmSettings::isDnd)
                .orElse(false);
    }

    public void sendTestNotificationByType(
            Long userId,
            String type,
            String captureId,
            String title,
            String body
    ) {
        if (isDndEnabled(userId)) {
            log.info("방해금지 설정으로 테스트 알림 전송 차단 userId={}", userId);
            return;
        }

        List<PushToken> tokens = pushTokenRepository.findAllByUserId(userId);

        if (tokens.isEmpty()) {
            log.warn("해당 유저({})에게 등록된 푸시 토큰이 없습니다.", userId);
            return;
        }

        String deeplink = "moa://captures/" + captureId;

        for (PushToken pushToken : tokens) {
            Message message = Message.builder()
                    .setToken(pushToken.getToken())
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build())
                    .putData("type", type)
                    .putData("capture_id", captureId)
                    .putData("title", title)
                    .putData("body", body)
                    .putData("deeplink", deeplink)
                    .build();

            try {
                FirebaseMessaging.getInstance().send(message);
                log.info("테스트 FCM 전송 성공 userId={}", userId);
            } catch (FirebaseMessagingException e) {
                log.error("테스트 FCM 전송 실패 userId={}: {}", userId, e.getMessage());
            }
        }
    }

    public boolean sendDeadlineReminder(
            Long userId,
            Long captureId,
            String title,
            LocalDate deadlineDate
    ) {

        if (isDndEnabled(userId)) {
            log.info("방해금지 설정으로 마감 알림 전송 차단 userId={}, captureId={}",
                    userId, captureId);
            return false;
        }

        List<PushToken> tokens = pushTokenRepository.findAllByUserId(userId);

        if (tokens.isEmpty()) {
            log.warn("마감 알림 전송 실패: 등록된 토큰 없음 userId={}", userId);
            return false;
        }

        String notificationTitle = "마감일 알림";
        String body = "'" + title + "' 항목이 오늘 마감돼요.";
        String captureIdValue = String.valueOf(captureId);
        String deeplink = "moa://captures/" + captureId;

        boolean sent = false;

        for (PushToken pushToken : tokens) {
            Message message = Message.builder()
                    .setToken(pushToken.getToken())
                    .setNotification(Notification.builder()
                            .setTitle(notificationTitle)
                            .setBody(body)
                            .build())
                    .putData("type", "DEADLINE_REMINDER")
                    .putData("capture_id", captureIdValue)
                    .putData("title", title)
                    .putData("deadline_date", deadlineDate.toString())
                    .putData("body", body)
                    .putData("deeplink", deeplink)
                    .build();

            try {
                FirebaseMessaging.getInstance().send(message);
                sent = true;
                log.info("마감 리마인드 전송 성공 userId={}, captureId={}",
                        userId, captureId);
            } catch (FirebaseMessagingException e) {
                log.error("마감 리마인드 전송 실패 userId={}, captureId={}: {}",
                        userId, captureId, e.getMessage());
            }
        }

        return sent;
    }

    public boolean sendLocationArrival(
            Long userId,
            Long captureId,
            String placeName
    ) {
        if (isDndEnabled(userId)) {
            log.info("방해금지 설정으로 장소 도착 알림 차단 userId={}, captureId={}",
                    userId, captureId);
            return false;
        }

        List<PushToken> tokens = pushTokenRepository.findAllByUserId(userId);

        if (tokens.isEmpty()) {
            log.warn("장소 도착 알림 전송 실패: 등록된 토큰 없음 userId={}", userId);
            return false;
        }

        String title = "장소 도착 알림";
        String body = "'" + placeName + "' 근처에 도착했어요.";
        String captureIdValue = String.valueOf(captureId);
        String deeplink = "moa://captures/" + captureId;

        boolean sent = false;

        for (PushToken pushToken : tokens) {
            Message message = Message.builder()
                    .setToken(pushToken.getToken())
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build())
                    .putData("type", "LOCATION_ARRIVAL")
                    .putData("capture_id", captureIdValue)
                    .putData("title", title)
                    .putData("body", body)
                    .putData("deeplink", deeplink)
                    .build();

            try {
                FirebaseMessaging.getInstance().send(message);
                sent = true;

                log.info("장소 도착 알림 전송 성공 userId={}, captureId={}",
                        userId, captureId);

            } catch (FirebaseMessagingException e) {
                log.error("장소 도착 알림 전송 실패 userId={}, captureId={}: {}",
                        userId, captureId, e.getMessage());
            }
        }

        return sent;
    }
}
