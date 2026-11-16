package com.moa.moa_be.domain.capture.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.moa.moa_be.domain.capture.dto.CompletionStatsResponse;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class CompletionStatsServiceTest {

    // 2026-11-15(일) 19:00 KST
    private static final Instant NOW = Instant.parse("2026-11-15T10:00:00Z");
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 15);

    @Test
    void noTargetsMeansNullRate() {
        CompletionStatsResponse stats = CompletionStatsService.calculate(List.of(), NOW, TODAY);
        assertNull(stats.rate());
        assertEquals(0, stats.doneCount());
        assertEquals(0, stats.todoCount());
        assertTrue(stats.weekly().isEmpty());
    }

    @Test
    void rateIsDoneOverDonePlusTodoRoundedToTwoDecimals() {
        Instant created = Instant.parse("2026-11-10T00:00:00Z");
        List<Instant[]> targets = List.of(
                new Instant[]{created, Instant.parse("2026-11-11T00:00:00Z")},
                new Instant[]{created, null}, new Instant[]{created, null},
                new Instant[]{created, null}, new Instant[]{created, null}, new Instant[]{created, null});

        CompletionStatsResponse stats = CompletionStatsService.calculate(targets, NOW, TODAY);

        assertEquals(0.17, stats.rate());
        assertEquals(1, stats.doneCount());
        assertEquals(5, stats.todoCount());
    }

    @Test
    void weeklyStartsOnMondayAndSkipsWeeksWithoutTargets() {
        List<Instant[]> targets = List.of(
                // 10/6(월) KST 에 만들고 11/3 에 완료
                new Instant[]{Instant.parse("2026-10-06T01:00:00Z"), Instant.parse("2026-11-03T01:00:00Z")},
                // 11/10 에 만들고 아직 대기
                new Instant[]{Instant.parse("2026-11-10T01:00:00Z"), null});

        CompletionStatsResponse stats = CompletionStatsService.calculate(targets, NOW, TODAY);

        // 최근 6주: 10/5, 10/12, 10/19, 10/26, 11/2, 11/9 (모두 월요일)
        List<CompletionStatsResponse.Weekly> weekly = stats.weekly();
        assertEquals(6, weekly.size());
        assertEquals(LocalDate.of(2026, 10, 5), weekly.get(0).weekStart());
        assertEquals(0.0, weekly.get(0).rate());
        assertEquals(LocalDate.of(2026, 11, 2), weekly.get(4).weekStart());
        assertEquals(1.0, weekly.get(4).rate());
        assertEquals(LocalDate.of(2026, 11, 9), weekly.get(5).weekStart());
        assertEquals(0.5, weekly.get(5).rate());
    }

    @Test
    void weeksBeforeFirstTargetAreOmitted() {
        List<Instant[]> targets = List.<Instant[]>of(new Instant[]{Instant.parse("2026-11-10T01:00:00Z"), null});
        List<CompletionStatsResponse.Weekly> weekly = CompletionStatsService.calculate(targets, NOW, TODAY).weekly();
        assertEquals(1, weekly.size());
        assertEquals(LocalDate.of(2026, 11, 9), weekly.get(0).weekStart());
    }
}
