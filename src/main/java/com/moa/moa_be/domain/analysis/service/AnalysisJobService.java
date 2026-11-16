package com.moa.moa_be.domain.analysis.service;

import com.moa.moa_be.domain.alarm.dto.CoordinateDto;
import com.moa.moa_be.domain.alarm.service.AlarmService;
import com.moa.moa_be.domain.alarm.service.GeocodingService;
import com.moa.moa_be.domain.analysis.config.AnalysisProperties;
import com.moa.moa_be.domain.analysis.dto.AnalysisJobResponse;
import com.moa.moa_be.domain.analysis.dto.AnalysisJobStatusResponse;
import com.moa.moa_be.domain.analysis.dto.AnalysisResult;
import com.moa.moa_be.domain.analysis.dto.CaptureAnalysisResponse;
import com.moa.moa_be.domain.analysis.entity.AnalysisStatus;
import com.moa.moa_be.domain.analysis.entity.ScreenshotAnalysis;
import com.moa.moa_be.domain.capture.dto.CaptureSummaryResponse;
import com.moa.moa_be.domain.capture.repository.CaptureQueryRepository;
import com.moa.moa_be.domain.capture.service.CaptureAssembler;
import com.moa.moa_be.domain.category.entity.Category;
import com.moa.moa_be.domain.category.repository.CategoryRepository;
import com.moa.moa_be.domain.category.service.CategoryService;
import com.moa.moa_be.domain.screenshot.entity.Classification;
import com.moa.moa_be.domain.screenshot.repository.ScreenshotClassificationRepository;
import com.moa.moa_be.global.exception.CustomException;
import com.moa.moa_be.global.exception.GlobalErrorCode;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import tools.jackson.databind.json.JsonMapper;

/**
 * 비동기 분석 흐름 (명세서 "AI 분석 API"):
 * 이미지 업로드 → 202 + jobId 즉시 응답 → 백그라운드에서 OCR·LLM·검증 → 결과 저장 → ANALYSIS_DONE 푸시.
 * 이미지는 메모리에서만 쓰고 저장하지 않는다 (분석 후 버림).
 */
@Slf4j
@Service
public class AnalysisJobService {

    static final long POLL_AFTER_MS = 2000;

    private final AnalysisService analysisService;
    private final AnalysisJobStore jobStore;
    private final AnalysisPushSender pushSender;
    private final CategoryService categoryService;
    private final CategoryRepository categoryRepository;
    private final AnalysisProperties properties;
    private final ThreadPoolTaskExecutor analysisExecutor;
    private final JsonMapper jsonMapper;
    private final ScreenshotClassificationRepository screenshotRepository;
    private final CaptureQueryRepository captureQueryRepository;
    private final CaptureAssembler captureAssembler;
    private final AlarmService alarmService;
    private final GeocodingService geocodingService;

    public AnalysisJobService(AnalysisService analysisService,
                              AnalysisJobStore jobStore,
                              AnalysisPushSender pushSender,
                              CategoryService categoryService,
                              CategoryRepository categoryRepository,
                              AnalysisProperties properties,
                              @Qualifier("analysisExecutor") ThreadPoolTaskExecutor analysisExecutor,
                              ObjectProvider<JsonMapper> jsonMapper,
                              ScreenshotClassificationRepository screenshotRepository,
                              CaptureQueryRepository captureQueryRepository,
                              CaptureAssembler captureAssembler,
                              AlarmService alarmService, GeocodingService geocodingService) {
        this.analysisService = analysisService;
        this.jobStore = jobStore;
        this.pushSender = pushSender;
        this.categoryService = categoryService;
        this.categoryRepository = categoryRepository;
        this.properties = properties;
        this.analysisExecutor = analysisExecutor;
        this.jsonMapper = jsonMapper.getIfAvailable(() -> JsonMapper.builder().build());
        this.screenshotRepository = screenshotRepository;
        this.captureQueryRepository = captureQueryRepository;
        this.captureAssembler = captureAssembler;
        this.alarmService = alarmService;
        this.geocodingService = geocodingService;
    }

    /** POST /captures/{id}/analysis */
    @Transactional
    public AnalysisJobResponse request(Long userId, Long captureId, MultipartFile image) {
        var capture = screenshotRepository.findOwnedForUpdate(captureId, userId)
                .orElseThrow(() -> new CustomException(GlobalErrorCode.NOT_FOUND));
        if (capture.getClassification() != Classification.PRACTICE) {
            throw new CustomException(GlobalErrorCode.INVALID_STATE_TRANSITION);
        }
        byte[] bytes = analysisService.readImage(image);
        ScreenshotAnalysis analysis = jobStore.createOrReset(userId, captureId, properties.dailyLimit());
        Long jobId = analysis.getId();

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    analysisExecutor.execute(() -> run(jobId, userId, captureId, bytes));
                } catch (TaskRejectedException e) {
                    log.error("분석 대기열이 가득 참 jobId={}", jobId);
                    // afterCommit에서 새 트랜잭션이 필요하므로 작업 스레드에 실패 처리를 위임한다.
                    java.util.concurrent.CompletableFuture.runAsync(() -> {
                        if (jobStore.fail(jobId, GlobalErrorCode.RATE_LIMITED.getCode(), "잠시 후 다시 시도해 주세요")) {
                            pushSender.sendAnalysisDone(userId, captureId, jobId, AnalysisStatus.FAILED);
                        }
                    });
                }
            }
        });
        return new AnalysisJobResponse(jobId, captureId, AnalysisStatus.PENDING, POLL_AFTER_MS);
    }

    /** 백그라운드 스레드에서 실행 */
    void run(Long jobId, Long userId, Long captureId, byte[] image) {
        AnalysisStatus finalStatus;
        try {
            AnalysisService.AnalysisRun run = analysisService.analyze(userId, image,
                    step -> jobStore.markStep(jobId, step));
            AnalysisResult result = run.result();

            String placeName = null;
            String placeAddress = null;
            Double latitude = null;
            Double longitude = null;

            if (result.place() != null
                    && result.place().address() != null
                    && !result.place().address().isBlank()) {

                placeName = result.place().name();
                placeAddress = result.place().address();

                log.info("장소 정보 추출 완료 name={}, address={}",
                        placeName, placeAddress);

                CoordinateDto coordinate =
                        geocodingService.getCoordinates(placeName, placeAddress);

                if (coordinate != null) {
                    latitude = coordinate.getLat();
                    longitude = coordinate.getLng();

                    log.info("좌표 변환 성공 lat={}, lng={}",
                            latitude, longitude);
                } else {
                    log.warn("좌표 변환 실패 name={}, address={}",
                            placeName, placeAddress);
                }
            }
            Long categoryId = categoryRepository.findByUserIdAndName(userId, result.category())
                    .map(Category::getId)
                    .orElse(null);
            boolean saved = jobStore.succeed(
                    jobId,
                    result,
                    categoryId,
                    jsonMapper.writeValueAsString(result),
                    placeName,
                    placeAddress,
                    latitude,
                    longitude
            );
            if (!saved) {
                return; // 이미 타임아웃으로 실패 처리됨 → 푸시도 이미 보냄
            }
            finalStatus = AnalysisStatus.SUCCEEDED;

            // 분석 완료 시 마감 알림 예약
            alarmService.scheduleDeadlineAlarm(
                    userId,
                    captureId,
                    result.title(),
                    result.deadline().status(),
                    result.deadline().date()
            );

        } catch (CustomException e) {
            if (!jobStore.fail(jobId, e.getErrorCode().getCode(), e.getErrorCode().getMessage())) {
                return;
            }
            finalStatus = AnalysisStatus.FAILED;
        } catch (Exception e) {
            log.error("분석 작업 중 예상치 못한 오류 jobId={}", jobId, e);
            if (!jobStore.fail(jobId, GlobalErrorCode.ANALYSIS_FAILED.getCode(),
                    GlobalErrorCode.ANALYSIS_FAILED.getMessage())) {
                return;
            }
            finalStatus = AnalysisStatus.FAILED;
        }
        pushSender.sendAnalysisDone(userId, captureId, jobId, finalStatus);
    }

    /** GET /analysis-jobs?ids= */
    public AnalysisJobStatusResponse getJobs(Long userId, List<Long> jobIds) {
        List<AnalysisJobStatusResponse.Job> jobs = jobStore.findMine(userId, jobIds).stream()
                .map(AnalysisJobStatusResponse.Job::from)
                .toList();
        return new AnalysisJobStatusResponse(jobs);
    }

    /** GET /captures/{id}/analysis — 실천 상세(C 파트)에서도 이 메서드를 쓰면 됨 */
    public CaptureAnalysisResponse getResult(Long userId, Long captureId) {
        return toResponse(userId, jobStore.getMine(userId, captureId));
    }

    /**
     * PATCH /captures/{id}/category (B-4) — 응답은 명세서대로 캡처 요약형.
     * <ul>
     *   <li>실천 사진: 분석 결과의 카테고리를 바꾸고 출처를 USER 로 (분석이 끝난 사진만, 아니면 409)</li>
     *   <li>참고 사진(안함으로 온 사진 포함): 캡처의 참고함 카테고리를 바꿈</li>
     * </ul>
     */
    @Transactional
    public CaptureSummaryResponse changeCategory(Long userId, Long captureId, Long categoryId) {
        categoryService.getMyCategory(userId, categoryId); // 내 카테고리가 아니면 404
        var capture = screenshotRepository.findOwnedForUpdate(captureId, userId)
                .orElseThrow(() -> new CustomException(GlobalErrorCode.NOT_FOUND));

        switch (capture.getClassification()) {
            case PRACTICE -> jobStore.changeCategory(userId, captureId, categoryId);
            case REFERENCE -> capture.changeReferenceCategory(categoryId);
            default -> throw new CustomException(GlobalErrorCode.NOT_FOUND); // 삭제 처리된 사진
        }

        return captureQueryRepository.findOne(userId, captureId)
                .map(row -> captureAssembler.toSummary(userId, row))
                .orElseThrow(() -> new CustomException(GlobalErrorCode.NOT_FOUND));
    }

    /**
     * A 파트에서 호출: 캡처를 참고로 바꾸거나 미분류로 되돌리면 분석 내용(제목·추출 정보·가이드·카테고리)을 삭제.
     * 나중에 다시 실천으로 바꾸면 처음부터 다시 분석한다.
     */
    public void deleteByCaptureId(Long captureId) {
        jobStore.deleteByCaptureId(captureId);
    }

    /** 회원 탈퇴 시 */
    public void deleteAllByUserId(Long userId) {
        jobStore.deleteAllByUserId(userId);
    }

    private CaptureAnalysisResponse toResponse(Long userId, ScreenshotAnalysis analysis) {
        CaptureAnalysisResponse.Category category = analysis.getCategoryId() == null ? null
                : categoryRepository.findByIdAndUserId(analysis.getCategoryId(), userId)
                .map(c -> new CaptureAnalysisResponse.Category(c.getId(), c.getName()))
                .orElse(null);

        AnalysisResult result = null;
        if (analysis.getStatus() == AnalysisStatus.SUCCEEDED && analysis.getResultJson() != null) {
            result = jsonMapper.readValue(analysis.getResultJson(), AnalysisResult.class);
            if (category != null && !category.name().equals(result.category())) {
                // 사용자가 카테고리를 바꿨으면 결과에도 현재 카테고리 이름을 보여줌
                result = withCategory(result, category.name());
            }
        }
        return new CaptureAnalysisResponse(analysis.getCaptureId(), analysis.getId(), analysis.getStatus(),
                category, analysis.getCategorySource(), analysis.getFinishedAt(), result);
    }

    private AnalysisResult withCategory(AnalysisResult r, String categoryName) {
        return new AnalysisResult(
                r.deadline(),
                categoryName,
                r.categoryConfidence(),
                r.title(),
                r.summary(),
                r.facts(),
                r.guide(),
                r.checklist(),
                r.place()
        );
    }
}