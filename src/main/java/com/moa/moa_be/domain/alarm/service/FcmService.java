package com.moa.moa_be.domain.alarm.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.moa.moa_be.domain.alarm.entity.PushToken;
import com.moa.moa_be.domain.alarm.repository.PushTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FcmService {

    private final PushTokenRepository pushTokenRepository;

    public void sendTestNotificationByType(Long userId, String type, String captureId) {
        List<PushToken> tokens = pushTokenRepository.findAllByUserId(userId);

        if (tokens.isEmpty()) {
            log.warn("해당 유저({})에게 등록된 푸시 토큰이 없습니다.", userId);
            return;
        }

        // TODO: 추후 Capture 도메인 연동 시, captureId를 이용해 DB에서 실제 캡처 항목의 title과 deadline 등을 조회해 오도록 수정 필요
        String title = "연남 소바실";
        String body = "내일까지예요";
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
                log.info("FCM 푸시 전송 성공 토큰: {}", pushToken.getToken());
            } catch (FirebaseMessagingException e) {
                log.error("FCM 푸시 전송 실패 토큰: {}, 에러: {}", pushToken.getToken(), e.getMessage());
            }
        }
    }
}