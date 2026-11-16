package com.moa.moa_be.domain.analysis.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.moa.moa_be.domain.alarm.entity.PushToken;
import com.moa.moa_be.domain.alarm.repository.PushTokenRepository;
import com.moa.moa_be.domain.analysis.entity.AnalysisStatus;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 분석이 끝나면(성공·실패 모두) 앱에 FCM 데이터 메시지 ANALYSIS_DONE 전송.
 * 화면에 알림은 띄우지 않고, 앱이 폴링을 멈추고 목록을 새로고침하는 용도.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AnalysisPushSender {

    private static final String TYPE = "ANALYSIS_DONE";

    private final PushTokenRepository pushTokenRepository;

    public void sendAnalysisDone(Long userId, Long captureId, Long jobId, AnalysisStatus status) {
        try {
            List<PushToken> tokens = pushTokenRepository.findAllByUserId(userId);
            for (PushToken token : tokens) {
                Message message = Message.builder()
                        .setToken(token.getToken())
                        .putData("type", TYPE)
                        .putData("capture_id", String.valueOf(captureId))
                        .putData("job_id", String.valueOf(jobId))
                        .putData("status", status.name())
                        .build();
                FirebaseMessaging.getInstance().send(message);
            }
        } catch (Exception e) {
            // 푸시는 실패해도 분석 결과에는 영향 없음 (앱은 폴링으로도 확인 가능)
            log.warn("ANALYSIS_DONE 푸시 전송 실패 userId={} captureId={}: {}", userId, captureId, e.getMessage());
        }
    }
}
