package com.moa.moa_be.domain.analysis.entity;

/** 분석 작업 상태 (API 명세서 GET /analysis-jobs 의 status) */
public enum AnalysisStatus {
    PENDING,    // 접수됨
    RUNNING,    // 분석 중
    SUCCEEDED,  // 성공
    FAILED;     // 실패 (다시 분석 가능)

    public boolean isInProgress() {
        return this == PENDING || this == RUNNING;
    }
}
