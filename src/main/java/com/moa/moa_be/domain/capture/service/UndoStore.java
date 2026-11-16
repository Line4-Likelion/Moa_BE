package com.moa.moa_be.domain.capture.service;

import com.moa.moa_be.domain.capture.entity.CaptureAction;
import com.moa.moa_be.domain.screenshot.entity.ScreenshotClassification.StateSnapshot;
import com.moa.moa_be.global.exception.CustomException;
import com.moa.moa_be.global.exception.GlobalErrorCode;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 되돌리기(C-11) 토큰 저장소. 5초만 유효하므로 서버 메모리에 둔다 (서버 1대 기준).
 * 캡처마다 가장 최근 동작의 토큰 하나만 유효하다.
 * 안함(DISMISS)의 분석 내용은 여기 복사하지 않고, DB에 남겨 둔 뒤 {@link DismissCleanupScheduler}가 지운다.
 */
@Component
public class UndoStore {

    public static final Duration TTL = Duration.ofSeconds(5);

    private static final SecureRandom RANDOM = new SecureRandom();

    private final Map<String, Entry> entries = new ConcurrentHashMap<>();
    private final Map<Long, String> latestByCapture = new ConcurrentHashMap<>();
    private final Clock clock;

    public UndoStore() {
        this(Clock.systemUTC());
    }

    UndoStore(Clock clock) {
        this.clock = clock;
    }

    public record Entry(String token, Long userId, Long captureId, CaptureAction action,
                        StateSnapshot previous, Instant expiresAt) {
    }

    /** 동작 직전 상태를 맡기고 토큰 발급. 같은 캡처의 이전 토큰은 무효가 된다. */
    public Entry issue(Long userId, Long captureId, CaptureAction action, StateSnapshot previous) {
        Entry entry = new Entry(newToken(), userId, captureId, action, previous, clock.instant().plus(TTL));
        entries.put(entry.token(), entry);
        String old = latestByCapture.put(captureId, entry.token());
        if (old != null) {
            entries.remove(old);
        }
        return entry;
    }

    /** 토큰을 쓰고 직전 상태를 돌려준다. 없거나·만료·다른 캡처·최신이 아니면 409. */
    public Entry consume(Long userId, Long captureId, String token) {
        Entry entry = entries.get(token);
        if (entry == null
                || !entry.userId().equals(userId)
                || !entry.captureId().equals(captureId)
                || !token.equals(latestByCapture.get(captureId))
                || !clock.instant().isBefore(entry.expiresAt())) {
            throw new CustomException(GlobalErrorCode.INVALID_STATE_TRANSITION);
        }
        entries.remove(token);
        latestByCapture.remove(captureId, token);
        return entry;
    }

    /** 캡처 삭제 시 */
    public void invalidate(Long captureId) {
        String token = latestByCapture.remove(captureId);
        if (token != null) {
            entries.remove(token);
        }
    }

    @Scheduled(fixedDelay = 10_000, initialDelay = 10_000)
    public void purgeExpired() {
        Instant now = clock.instant();
        entries.values().removeIf(entry -> {
            boolean expired = !now.isBefore(entry.expiresAt());
            if (expired) {
                latestByCapture.remove(entry.captureId(), entry.token());
            }
            return expired;
        });
    }

    int size() {
        return entries.size();
    }

    private static String newToken() {
        byte[] bytes = new byte[9];
        RANDOM.nextBytes(bytes);
        return "und_" + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
