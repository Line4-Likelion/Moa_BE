package com.moa.moa_be.domain.capture.service;

import com.moa.moa_be.domain.capture.dto.CompletionStatsResponse;
import com.moa.moa_be.domain.capture.repository.CaptureQueryRepository;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * GET /stats/completion (C-15). 완료율 = 완료 ÷ (완료 + 실천 대기).
 * 안함(참고함으로 이동)과 분석 중·실패 항목은 넣지 않는다.
 * weekly: 최근 6주(월요일 시작)마다 그 주가 끝난 시점(이번 주는 지금)의 누적 완료율. 대상이 없는 주는 뺀다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CompletionStatsService {

    static final int WEEKS = 6;

    private final CaptureQueryRepository captureQueryRepository;

    public CompletionStatsResponse get(Long userId) {
        return calculate(captureQueryRepository.findCompletionTargets(userId), Instant.now(), CaptureTimes.today());
    }

    /** @param targets [실천 분류 시각, 완료 시각(없으면 null)] */
    static CompletionStatsResponse calculate(List<Instant[]> targets, Instant now, LocalDate today) {
        long done = targets.stream().filter(t -> t[1] != null).count();
        long todo = targets.size() - done;

        List<CompletionStatsResponse.Weekly> weekly = new ArrayList<>();
        LocalDate thisWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        for (int i = WEEKS - 1; i >= 0; i--) {
            LocalDate weekStart = thisWeek.minusWeeks(i);
            Instant end = i == 0 ? now : CaptureTimes.startOf(weekStart.plusWeeks(1));
            long total = targets.stream().filter(t -> t[0].isBefore(end)).count();
            long doneByEnd = targets.stream().filter(t -> t[0].isBefore(end) && t[1] != null && t[1].isBefore(end))
                    .count();
            if (total > 0) {
                weekly.add(new CompletionStatsResponse.Weekly(weekStart, round(doneByEnd, total)));
            }
        }
        return new CompletionStatsResponse(done + todo == 0 ? null : round(done, done + todo), done, todo, weekly);
    }

    private static double round(long part, long whole) {
        return Math.round(part * 100.0 / whole) / 100.0;
    }
}
