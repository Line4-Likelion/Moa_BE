package com.moa.moa_be.domain.analysis.ocr;

/**
 * OCR 업체를 바꾸면 이 인터페이스의 구현만 교체 (현재: Google Cloud Vision).
 */
public interface OcrClient {

    OcrResult read(byte[] image);
}
