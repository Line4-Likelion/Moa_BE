package com.moa.moa_be.domain.analysis.service;

/**
 * OCR·LLM 호출이나 응답 처리 중 실패. AnalysisService 에서 ANALYSIS_FAILED 로 변환된다.
 */
public class AnalysisException extends RuntimeException {

    public AnalysisException(String message) {
        super(message);
    }

    public AnalysisException(String message, Throwable cause) {
        super(message, cause);
    }
}
