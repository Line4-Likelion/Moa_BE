package com.moa.moa_be.domain.capture.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.moa.moa_be.domain.capture.entity.CaptureAction;
import com.moa.moa_be.domain.screenshot.entity.Classification;
import com.moa.moa_be.domain.screenshot.entity.ScreenshotClassification.StateSnapshot;
import com.moa.moa_be.global.exception.CustomException;
import com.moa.moa_be.global.exception.GlobalErrorCode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

class UndoStoreTest {

    private static final StateSnapshot PREVIOUS =
            new StateSnapshot(Classification.PRACTICE, null, null, false, Instant.parse("2026-11-01T00:00:00Z"), null);

    private MutableClock clock;
    private UndoStore store;

    @BeforeEach
    void setUp() {
        clock = new MutableClock(Instant.parse("2026-11-15T10:00:00Z"));
        store = new UndoStore(clock);
    }

    @Test
    void consumeWithinFiveSecondsReturnsPreviousState() {
        UndoStore.Entry entry = store.issue(1L, 10L, CaptureAction.COMPLETE, PREVIOUS);
        assertTrue(entry.token().startsWith("und_"));
        assertEquals(clock.instant().plusSeconds(5), entry.expiresAt());

        clock.advance(Duration.ofMillis(4_999));
        assertEquals(PREVIOUS, store.consume(1L, 10L, entry.token()).previous());
    }

    @Test
    void expiredTokenIs409() {
        UndoStore.Entry entry = store.issue(1L, 10L, CaptureAction.COMPLETE, PREVIOUS);
        clock.advance(Duration.ofSeconds(5));
        assertConflict(() -> store.consume(1L, 10L, entry.token()));
    }

    @Test
    void unknownTokenIs409() {
        store.issue(1L, 10L, CaptureAction.COMPLETE, PREVIOUS);
        assertConflict(() -> store.consume(1L, 10L, "und_wrong"));
    }

    @Test
    void tokenOfAnotherCaptureOrUserIs409() {
        UndoStore.Entry entry = store.issue(1L, 10L, CaptureAction.COMPLETE, PREVIOUS);
        assertConflict(() -> store.consume(1L, 11L, entry.token()));
        assertConflict(() -> store.consume(2L, 10L, entry.token()));
        // 잘못된 시도 후에도 원래 캡처에서는 쓸 수 있음
        assertEquals(PREVIOUS, store.consume(1L, 10L, entry.token()).previous());
    }

    @Test
    void tokenCanBeUsedOnlyOnce() {
        UndoStore.Entry entry = store.issue(1L, 10L, CaptureAction.DISMISS, PREVIOUS);
        store.consume(1L, 10L, entry.token());
        assertConflict(() -> store.consume(1L, 10L, entry.token()));
    }

    @Test
    void newActionInvalidatesPreviousToken() {
        UndoStore.Entry first = store.issue(1L, 10L, CaptureAction.SNOOZE, PREVIOUS);
        UndoStore.Entry second = store.issue(1L, 10L, CaptureAction.COMPLETE, PREVIOUS);
        assertConflict(() -> store.consume(1L, 10L, first.token()));
        assertEquals(CaptureAction.COMPLETE, store.consume(1L, 10L, second.token()).action());
    }

    @Test
    void purgeRemovesOnlyExpired() {
        store.issue(1L, 10L, CaptureAction.COMPLETE, PREVIOUS);
        clock.advance(Duration.ofSeconds(3));
        UndoStore.Entry fresh = store.issue(1L, 11L, CaptureAction.COMPLETE, PREVIOUS);
        clock.advance(Duration.ofSeconds(3));

        store.purgeExpired();

        assertEquals(1, store.size());
        assertEquals(PREVIOUS, store.consume(1L, 11L, fresh.token()).previous());
    }

    private static void assertConflict(Executable executable) {
        CustomException e = assertThrows(CustomException.class, executable);
        assertEquals(GlobalErrorCode.INVALID_STATE_TRANSITION, e.getErrorCode());
    }

    private static final class MutableClock extends Clock {
        private Instant now;

        MutableClock(Instant now) {
            this.now = now;
        }

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
