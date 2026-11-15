package com.moa.moa_be.domain.analysis.service;

import com.moa.moa_be.domain.analysis.entity.AnalysisStatus;
import com.moa.moa_be.domain.analysis.entity.ScreenshotAnalysis;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 명세서: 120초 안에 안 끝나면 ANALYSIS_TIMEOUT 으로 실패 처리.
 * 서버가 분석 도중 재시작돼서 RUNNING 으로 남은 작업도 여기서 정리된다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AnalysisTimeoutChecker {

    static final long TIMEOUT_SECONDS = 120;

    private final AnalysisJobStore jobStore;
    private final AnalysisPushSender pushSender;

    @Scheduled(fixedDelay = 15_000, initialDelay = 15_000)
    public void failTimedOutJobs() {
        List<ScreenshotAnalysis> timedOut = jobStore.failTimedOut(TIMEOUT_SECONDS);
        for (ScreenshotAnalysis a : timedOut) {
            log.warn("분석 타임아웃 jobId={} captureId={}", a.getId(), a.getCaptureId());
            pushSender.sendAnalysisDone(a.getUserId(), a.getCaptureId(), a.getId(), AnalysisStatus.FAILED);
        }
    }
}
