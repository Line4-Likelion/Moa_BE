package com.moa.moa_be.domain.analysis.service;

import com.moa.moa_be.domain.analysis.dto.AnalysisResult;
import com.moa.moa_be.domain.analysis.llm.AnalysisClient;
import com.moa.moa_be.domain.analysis.llm.ImageInput;
import com.moa.moa_be.domain.analysis.llm.LlmAnalysis;
import com.moa.moa_be.domain.analysis.ocr.OcrClient;
import com.moa.moa_be.domain.analysis.ocr.OcrResult;
import com.moa.moa_be.domain.category.service.CategoryService;
import com.moa.moa_be.global.exception.CustomException;
import com.moa.moa_be.global.exception.GlobalErrorCode;
import java.io.IOException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;
import com.moa.moa_be.domain.analysis.entity.AnalysisStep;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * 분석 순서 (명세서 "분석 순서와 규칙"):
 * OCR → (마감기한 판단 · 카테고리 · 실천 방법을 LLM 한 번에) → 서버 검증
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AnalysisService {

    public static final long MAX_IMAGE_BYTES = 10L * 1024 * 1024;
    private static final Set<String> ALLOWED_TYPES = Set.of("image/jpeg", "image/jpg", "image/png", "image/webp");
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final int LLM_MAX_ATTEMPTS = 2; // JSON 이 아니면 한 번 다시 요청
    /** OCR 글자(공백 제외)가 이보다 적으면 이미지도 같이 LLM 에 보냄 */
    static final int MIN_OCR_CHARS_FOR_TEXT_ONLY = 20;

    private final OcrClient ocrClient;
    private final AnalysisClient analysisClient;
    private final AnalysisValidator validator;
    private final CategoryService categoryService;

    /** 분석 1회 결과 (개발용 API 에서 OCR 원문도 같이 보여주려고 묶음) */
    public record AnalysisRun(OcrResult ocr, AnalysisResult result, boolean imageSent, long elapsedMs) {
    }

    public AnalysisRun analyze(Long userId, MultipartFile image) {
        return analyze(userId, readImage(image));
    }

    public AnalysisRun analyze(Long userId, byte[] image) {
        return analyze(userId, image, step -> { });
    }

    /** @param onStep 단계가 바뀔 때마다 호출 (비동기 작업의 step 표시용) */
    public AnalysisRun analyze(Long userId, byte[] image, Consumer<AnalysisStep> onStep) {
        long start = System.currentTimeMillis();
        try {
            onStep.accept(AnalysisStep.OCR);
            OcrResult ocr = ocrClient.read(image);
            // 글자가 거의 없는 사진(옷·화장품·음식 사진 등)은 이미지도 같이 보냄
            ImageInput imageInput = needsImage(ocr) ? ImageInput.of(image) : null;
            List<String> categories = categoryService.getCategoryNames(userId);
            onStep.accept(AnalysisStep.CATEGORIZE);
            LlmAnalysis raw = callLlm(ocr, categories, imageInput);
            AnalysisResult result = validator.validate(raw, ocr, categories, imageInput != null);

            long elapsed = System.currentTimeMillis() - start;
            log.info("분석 완료 userId={} blocks={} image={} category={} deadline={} {}ms",
                    userId, ocr.blocks().size(), imageInput != null, result.category(),
                    result.deadline().status(), elapsed);
            return new AnalysisRun(ocr, result, imageInput != null, elapsed);
        } catch (AnalysisException e) {
            log.error("분석 실패 userId={}: {}", userId, e.getMessage());
            throw new CustomException(GlobalErrorCode.ANALYSIS_FAILED);
        }
    }

    private boolean needsImage(OcrResult ocr) {
        return ocr.fullText().replaceAll("\\s+", "").length() < MIN_OCR_CHARS_FOR_TEXT_ONLY;
    }

    private LlmAnalysis callLlm(OcrResult ocr, List<String> categories, ImageInput image) {
        LocalDate today = LocalDate.now(KST);
        AnalysisException last = null;
        for (int attempt = 1; attempt <= LLM_MAX_ATTEMPTS; attempt++) {
            try {
                return analysisClient.analyze(ocr, categories, today, image);
            } catch (AnalysisException e) {
                last = e;
                log.warn("LLM 분석 실패 ({}/{}): {}", attempt, LLM_MAX_ATTEMPTS, e.getMessage());
            }
        }
        throw last;
    }

    public byte[] readImage(MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw new CustomException(GlobalErrorCode.VALIDATION_ERROR);
        }
        if (image.getSize() > MAX_IMAGE_BYTES) {
            throw new CustomException(GlobalErrorCode.IMAGE_TOO_LARGE);
        }
        if (!isAllowedType(image)) {
            throw new CustomException(GlobalErrorCode.UNSUPPORTED_IMAGE);
        }
        try {
            return image.getBytes();
        } catch (IOException e) {
            throw new CustomException(GlobalErrorCode.UNSUPPORTED_IMAGE);
        }
    }

    private boolean isAllowedType(MultipartFile image) {
        String contentType = image.getContentType();
        if (contentType != null && ALLOWED_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            return true;
        }
        String name = image.getOriginalFilename();
        if (name == null || !name.contains(".")) {
            return false;
        }
        String ext = name.substring(name.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        return ALLOWED_EXTENSIONS.contains(ext);
    }
}
