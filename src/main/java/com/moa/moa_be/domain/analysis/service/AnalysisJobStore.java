package com.moa.moa_be.domain.analysis.service;

import com.moa.moa_be.domain.analysis.dto.AnalysisResult;
import com.moa.moa_be.domain.analysis.entity.AnalysisStatus;
import com.moa.moa_be.domain.analysis.entity.AnalysisStep;
import com.moa.moa_be.domain.analysis.entity.ScreenshotAnalysis;
import com.moa.moa_be.domain.analysis.repository.ScreenshotAnalysisRepository;
import com.moa.moa_be.global.exception.CustomException;
import com.moa.moa_be.global.exception.GlobalErrorCode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 분석 작업의 DB 상태 변경을 짧은 트랜잭션 단위로 처리.
 * (OCR·LLM 호출처럼 오래 걸리는 작업 중에 트랜잭션을 잡고 있지 않으려고 따로 분리)
 */
@Component
@RequiredArgsConstructor
public class AnalysisJobStore {

    private final ScreenshotAnalysisRepository repository;

    /** 분석 접수: 처음이면 생성, 다시 분석이면 초기화. */
    @Transactional
    public ScreenshotAnalysis createOrReset(Long userId, Long captureId, int dailyLimit) {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        if (repository.countByUserIdAndRequestedAtGreaterThanEqual(userId, todayStart) >= dailyLimit) {
            throw new CustomException(GlobalErrorCode.RATE_LIMITED);
        }

        ScreenshotAnalysis analysis = repository.findByCaptureId(captureId).orElse(null);
        if (analysis == null) {
            return repository.save(ScreenshotAnalysis.request(userId, captureId));
        }
        if (!analysis.getUserId().equals(userId)) {
            throw new CustomException(GlobalErrorCode.FORBIDDEN);
        }
        if (analysis.getStatus().isInProgress() || !analysis.canReanalyze()) {
            // 이미 분석 중이거나, 성공해서 가이드까지 있는 경우
            throw new CustomException(GlobalErrorCode.INVALID_STATE_TRANSITION);
        }
        analysis.reset();
        return analysis;
    }

    @Transactional
    public void markStep(Long analysisId, AnalysisStep step) {
        repository.findById(analysisId)
                .filter(a -> a.getStatus().isInProgress())
                .ifPresent(a -> a.start(step));
    }

    /** @return 저장했으면 true (이미 타임아웃 처리된 작업이면 false) */
    @Transactional
    public boolean succeed(Long analysisId, AnalysisResult result, Long categoryId, String resultJson) {
        ScreenshotAnalysis analysis = repository.findById(analysisId).orElse(null);
        if (analysis == null || !analysis.getStatus().isInProgress()) {
            return false;
        }
        analysis.succeed(result.deadline().status(), result.deadline().date(), categoryId,
                result.title(), result.guide() != null, resultJson);
        return true;
    }

    @Transactional
    public boolean fail(Long analysisId, String errorCode, String errorMessage) {
        ScreenshotAnalysis analysis = repository.findById(analysisId).orElse(null);
        if (analysis == null || !analysis.getStatus().isInProgress()) {
            return false;
        }
        analysis.fail(errorCode, errorMessage);
        return true;
    }

    /** 접수 후 timeoutSeconds 가 지나도 안 끝난 작업을 ANALYSIS_TIMEOUT 으로 실패 처리 */
    @Transactional
    public List<ScreenshotAnalysis> failTimedOut(long timeoutSeconds) {
        LocalDateTime before = LocalDateTime.now().minusSeconds(timeoutSeconds);
        List<ScreenshotAnalysis> stale = repository.findAllByStatusInAndRequestedAtBefore(
                List.of(AnalysisStatus.PENDING, AnalysisStatus.RUNNING), before);
        stale.forEach(a -> a.fail(GlobalErrorCode.ANALYSIS_TIMEOUT.getCode(),
                "분석 요청은 접수됐지만 분석이 중단됐어요"));
        return stale;
    }

    @Transactional
    public ScreenshotAnalysis changeCategory(Long userId, Long captureId, Long categoryId) {
        ScreenshotAnalysis analysis = getMine(userId, captureId);
        if (analysis.getStatus() != AnalysisStatus.SUCCEEDED) {
            throw new CustomException(GlobalErrorCode.INVALID_STATE_TRANSITION);
        }
        analysis.changeCategory(categoryId);
        return analysis;
    }

    @Transactional(readOnly = true)
    public ScreenshotAnalysis getMine(Long userId, Long captureId) {
        ScreenshotAnalysis analysis = repository.findByCaptureId(captureId)
                .orElseThrow(() -> new CustomException(GlobalErrorCode.NOT_FOUND));
        if (!analysis.getUserId().equals(userId)) {
            throw new CustomException(GlobalErrorCode.FORBIDDEN);
        }
        return analysis;
    }

    @Transactional(readOnly = true)
    public List<ScreenshotAnalysis> findMine(Long userId, List<Long> analysisIds) {
        return repository.findAllByIdInAndUserId(analysisIds, userId);
    }

    @Transactional
    public void deleteByCaptureId(Long captureId) {
        repository.deleteByCaptureId(captureId);
    }

    @Transactional
    public void deleteAllByUserId(Long userId) {
        repository.deleteAllByUserId(userId);
    }
}
