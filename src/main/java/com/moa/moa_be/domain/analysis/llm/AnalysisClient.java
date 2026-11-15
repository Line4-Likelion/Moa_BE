package com.moa.moa_be.domain.analysis.llm;

import com.moa.moa_be.domain.analysis.ocr.OcrResult;
import java.time.LocalDate;
import java.util.List;

/**
 * LLM 업체를 바꾸면 이 인터페이스의 구현만 교체 (현재: OpenAI).
 * 반환값은 검증 전 원본이므로 반드시 AnalysisValidator 를 거쳐야 한다.
 */
public interface AnalysisClient {

    /**
     * @param image 글자가 적은 사진이면 원본 이미지, 아니면 null (텍스트만 전송)
     */
    LlmAnalysis analyze(OcrResult ocr, List<String> categories, LocalDate today, ImageInput image);
}
