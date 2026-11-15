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
 * 체크한 체크리스트 항목 (C-05). 체크리스트 원문은 분석 결과(result_json)에 있고 여기엔 체크 여부만 둔다.
 * itemNo 는 분석 결과 checklist 의 1부터 시작하는 순번 (응답 id = "chk_" + itemNo).
 * 다시 분석하면 체크리스트가 바뀌므로 분석 접수(requestedAt)보다 먼저 기록된 체크는 무시한다.
 */
@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "capture_checklist_checks", uniqueConstraints =
        @UniqueConstraint(name = "uk_checklist_capture_item", columnNames = {"capture_id", "item_no"}))
public class ChecklistCheck {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "capture_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private ScreenshotClassification capture;

    @Column(name = "item_no", nullable = false)
    private int itemNo;

    @Column(name = "recorded_at", nullable = false)
    private LocalDateTime recordedAt;

    public ChecklistCheck(ScreenshotClassification capture, int itemNo) {
        this.capture = capture;
        this.itemNo = itemNo;
        this.recordedAt = LocalDateTime.now();
    }
}
