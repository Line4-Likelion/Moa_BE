package com.moa.moa_be.domain.analysis.service;

import com.moa.moa_be.domain.analysis.config.AnalysisProperties;
import com.moa.moa_be.domain.analysis.dto.AnalysisJobResponse;
import com.moa.moa_be.domain.analysis.dto.AnalysisJobStatusResponse;
import com.moa.moa_be.domain.analysis.dto.AnalysisResult;
import com.moa.moa_be.domain.analysis.dto.CaptureAnalysisResponse;
import com.moa.moa_be.domain.analysis.entity.AnalysisStatus;
import com.moa.moa_be.domain.analysis.entity.ScreenshotAnalysis;
import com.moa.moa_be.domain.category.entity.Category;
import com.moa.moa_be.domain.category.repository.CategoryRepository;
import com.moa.moa_be.domain.category.service.CategoryService;
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

    public AnalysisJobService(AnalysisService analysisService,
                              AnalysisJobStore jobStore,
                              AnalysisPushSender pushSender,
                              CategoryService categoryService,
                              CategoryRepository categoryRepository,
                              AnalysisProperties properties,
                              @Qualifier("analysisExecutor") ThreadPoolTaskExecutor analysisExecutor,
                              ObjectProvider<JsonMapper> jsonMapper) {
        this.analysisService = analysisService;
        this.jobStore = jobStore;
        this.pushSender = pushSender;
        this.categoryService = categoryService;
        this.categoryRepository = categoryRepository;
        this.properties = properties;
        this.analysisExecutor = analysisExecutor;
        this.jsonMapper = jsonMapper.getIfAvailable(() -> JsonMapper.builder().build());
    }

    /** POST /captures/{id}/analysis */
    public AnalysisJobResponse request(Long userId, Long captureId, MultipartFile image) {
        // TODO(A 파트 머지 후): captureId 가 내 캡처인지, 실천(좌 스와이프)으로 분류된 캡처인지 확인
        byte[] bytes = analysisService.readImage(image);
        ScreenshotAnalysis analysis = jobStore.createOrReset(userId, captureId, properties.dailyLimit());
        Long jobId = analysis.getId();

        try {
            analysisExecutor.execute(() -> run(jobId, userId, captureId, bytes));
        } catch (TaskRejectedException e) {
            log.error("분석 대기열이 가득 참 jobId={}", jobId);
            jobStore.fail(jobId, GlobalErrorCode.RATE_LIMITED.getCode(), "잠시 후 다시 시도해 주세요");
            throw new CustomException(GlobalErrorCode.RATE_LIMITED);
        }
        return new AnalysisJobResponse(jobId, captureId, AnalysisStatus.PENDING, POLL_AFTER_MS);
    }

    /** 백그라운드 스레드에서 실행 */
    void run(Long jobId, Long userId, Long captureId, byte[] image) {
        AnalysisStatus finalStatus;
        try {
            AnalysisService.AnalysisRun run = analysisService.analyze(userId, image,
                    step -> jobStore.markStep(jobId, step));
            AnalysisResult result = run.result();
            Long categoryId = categoryRepository.findByUserIdAndName(userId, result.category())
                    .map(Category::getId)
                    .orElse(null);
            boolean saved = jobStore.succeed(jobId, result, categoryId, jsonMapper.writeValueAsString(result));
            if (!saved) {
                return; // 이미 타임아웃으로 실패 처리됨 → 푸시도 이미 보냄
            }
            finalStatus = AnalysisStatus.SUCCEEDED;
            // TODO(D 파트): 분석 완료 시 알림 예약 생성 (마감기한 기반 리마인드)
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

    /** PATCH /captures/{id}/category (B-4) */
    public CaptureAnalysisResponse changeCategory(Long userId, Long captureId, Long categoryId) {
        categoryService.getMyCategory(userId, categoryId); // 내 카테고리가 아니면 404
        ScreenshotAnalysis analysis = jobStore.changeCategory(userId, captureId, categoryId);
        return toResponse(userId, analysis);
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
        return new AnalysisResult(r.deadline(), categoryName, r.categoryConfidence(), r.title(), r.summary(),
                r.facts(), r.guide(), r.checklist());
    }
}
