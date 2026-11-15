package com.moa.moa_be.domain.capture.service;

import com.moa.moa_be.domain.analysis.repository.ScreenshotAnalysisRepository;
import com.moa.moa_be.domain.capture.repository.CaptureQueryRepository;
import com.moa.moa_be.domain.capture.repository.ChecklistCheckRepository;
import com.moa.moa_be.domain.capture.repository.FactOverrideRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 안함(DISMISS) 항목의 분석 내용 삭제.
 * 안함 직후 5초 동안은 되돌릴 수 있어야 하므로 바로 지우지 않고(응답에서는 이미 숨김),
 * 되돌리기 시간이 넉넉히 지난 뒤 DB 에서 지운다. 서버가 재시작돼도 DB 기준이라 빠지지 않는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DismissCleanupScheduler {

    /** 되돌리기 유효 시간(5초)보다 넉넉하게 */
    static final Duration GRACE = Duration.ofSeconds(10);

    private final CaptureQueryRepository captureQueryRepository;
    private final ScreenshotAnalysisRepository analysisRepository;
    private final ChecklistCheckRepository checklistCheckRepository;
    private final FactOverrideRepository factOverrideRepository;

    @Scheduled(fixedDelay = 10_000, initialDelay = 10_000)
    @Transactional
    public void purgeDismissedAnalyses() {
        List<Long> captureIds = captureQueryRepository.findDismissedWithAnalysis(Instant.now().minus(GRACE));
        for (Long captureId : captureIds) {
            analysisRepository.deleteByCaptureId(captureId);
            checklistCheckRepository.deleteAllByCaptureId(captureId);
            factOverrideRepository.deleteAllByCaptureId(captureId);
        }
        if (!captureIds.isEmpty()) {
            log.info("안함 처리된 캡처 {}건의 분석 내용을 삭제했습니다.", captureIds.size());
        }
    }
}
