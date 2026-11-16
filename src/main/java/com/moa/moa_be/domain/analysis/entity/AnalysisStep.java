package com.moa.moa_be.domain.analysis.entity;

/**
 * 분석 진행 단계 (API 명세서 step).
 * 마감기한 판단·카테고리·가이드는 LLM 한 번 호출로 같이 처리해서 CATEGORIZE 하나로 표시.
 */
public enum AnalysisStep {
    OCR,        // 원문 정보 확인
    CATEGORIZE, // 마감기한 판단 · 카테고리 정리 · 실천 가이드 만들기
    DONE
}
