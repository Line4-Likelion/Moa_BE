package com.moa.moa_be.domain.capture.entity;

import com.moa.moa_be.domain.screenshot.entity.ScreenshotClassification;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

/**
 * 사용자가 고친 핵심 정보 (C-07). AI 원본은 분석 결과(result_json)에 그대로 두고 고친 값만 여기에 둔다.
 * 행을 지우면 AI 값으로 되돌아간다. 다시 분석하면 분석 접수(requestedAt)보다 먼저 기록된 값은 무시한다.
 */
@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "capture_fact_overrides", uniqueConstraints =
        @UniqueConstraint(name = "uk_fact_capture_key", columnNames = {"capture_id", "fact_key"}))
public class FactOverride {

    public static final int MAX_VALUE_LENGTH = 200;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "capture_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private ScreenshotClassification capture;

    @Column(name = "fact_key", nullable = false, length = 50)
    private String factKey;

    @Column(name = "fact_value", nullable = false, length = MAX_VALUE_LENGTH)
    private String value;

    @Column(name = "recorded_at", nullable = false)
    private LocalDateTime recordedAt;

    public FactOverride(ScreenshotClassification capture, String factKey, String value) {
        this.capture = capture;
        this.factKey = factKey;
        change(value);
    }

    public void change(String value) {
        this.value = value;
        this.recordedAt = LocalDateTime.now();
    }
}
