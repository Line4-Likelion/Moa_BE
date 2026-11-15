package com.moa.moa_be.domain.analysis.entity;

import com.moa.moa_be.domain.analysis.dto.AnalysisResult.DeadlineStatus;
import com.moa.moa_be.global.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 캡처 1장의 AI 분석 결과 (= 분석 작업). 캡처 하나당 한 행, 다시 분석하면 같은 행을 초기화해서 재사용.
 *
 * <p>캡처(분류) 자체는 A 파트의 screenshot_classifications 테이블이 담당하고,
 * 여기서는 그 id 를 captureId 로만 들고 있다. (A 파트 머지 후 FK 연결 예정)
 */
@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "screenshot_analyses",
        indexes = {
                @Index(name = "uk_analysis_capture", columnList = "capture_id", unique = true),
                @Index(name = "idx_analysis_user_category", columnList = "user_id, category_id"),
                @Index(name = "idx_analysis_status_started", columnList = "status, started_at")
        }
)
public class ScreenshotAnalysis extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "analysis_id")
    private Long id; // = 명세서의 job_id

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "capture_id", nullable = false)
    private Long captureId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AnalysisStatus status;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private AnalysisStep step;

    @Column(name = "error_code", length = 50)
    private String errorCode;

    @Column(name = "error_message")
    private String errorMessage;

    // ---- 분석 결과 (목록·필터·알림 계산용으로 컬럼에도 꺼내 둠) ----

    @Enumerated(EnumType.STRING)
    @Column(name = "deadline_status", length = 20)
    private DeadlineStatus deadlineStatus;

    @Column(name = "deadline_date")
    private LocalDate deadlineDate;

    @Column(name = "category_id")
    private Long categoryId;

    @Enumerated(EnumType.STRING)
    @Column(name = "category_source", length = 10)
    private CategorySource categorySource;

    @Column(length = 50)
    private String title;

    @Column(name = "has_guide")
    private Boolean hasGuide;

    /** AnalysisResult 전체 (추출 정보, 가이드, 체크리스트, 좌표 등) */
    @Column(name = "result_json", columnDefinition = "json")
    private String resultJson;

    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    public static ScreenshotAnalysis request(Long userId, Long captureId) {
        ScreenshotAnalysis analysis = new ScreenshotAnalysis();
        analysis.userId = userId;
        analysis.captureId = captureId;
        analysis.reset();
        return analysis;
    }

    /** 새로 접수하거나 다시 분석할 때 이전 결과를 모두 지움 */
    public void reset() {
        this.status = AnalysisStatus.PENDING;
        this.step = null;
        this.errorCode = null;
        this.errorMessage = null;
        this.deadlineStatus = null;
        this.deadlineDate = null;
        this.categoryId = null;
        this.categorySource = null;
        this.title = null;
        this.hasGuide = null;
        this.resultJson = null;
        this.requestedAt = LocalDateTime.now();
        this.startedAt = null;
        this.finishedAt = null;
    }

    /** 다시 분석해도 되는 상태인지: 실패했거나, 성공했지만 실천 방법(가이드)을 못 만든 경우 */
    public boolean canReanalyze() {
        return status == AnalysisStatus.FAILED
                || (status == AnalysisStatus.SUCCEEDED && !Boolean.TRUE.equals(hasGuide));
    }

    public void start(AnalysisStep step) {
        this.status = AnalysisStatus.RUNNING;
        this.step = step;
        if (this.startedAt == null) {
            this.startedAt = LocalDateTime.now();
        }
    }

    public void succeed(DeadlineStatus deadlineStatus, LocalDate deadlineDate, Long categoryId,
                        String title, boolean hasGuide, String resultJson) {
        this.status = AnalysisStatus.SUCCEEDED;
        this.step = AnalysisStep.DONE;
        this.deadlineStatus = deadlineStatus;
        this.deadlineDate = deadlineDate;
        this.categoryId = categoryId;
        this.categorySource = CategorySource.AI;
        this.title = title;
        this.hasGuide = hasGuide;
        this.resultJson = resultJson;
        this.finishedAt = LocalDateTime.now();
    }

    public void fail(String errorCode, String errorMessage) {
        this.status = AnalysisStatus.FAILED;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.finishedAt = LocalDateTime.now();
    }

    /** B-4: 사용자가 카테고리를 직접 변경 */
    public void changeCategory(Long categoryId) {
        this.categoryId = categoryId;
        this.categorySource = CategorySource.USER;
    }
}
