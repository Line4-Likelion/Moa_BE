package com.moa.moa_be.domain.screenshot.entity;

import com.moa.moa_be.domain.user.entity.User;
import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

@Entity
@Table(name = "screenshot_classifications", uniqueConstraints =
        @UniqueConstraint(name = "uk_screenshot_user_file", columnNames = {"user_id", "file_key"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ScreenshotClassification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "file_key", nullable = false, length = 255)
    private String fileKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Classification classification;

    @Column(nullable = false, updatable = false)
    private Instant classifiedAt;

    // ---- 실천함·참고함 상태 (C 파트) ----

    /** 완료(C-08) 시각. 값이 있으면 DONE */
    @Column(name = "completed_at")
    private Instant completedAt;

    /** 보류(C-09) 기한. 보류는 별도 상태가 아니라 TODO + snooze_until */
    @Column(name = "snooze_until")
    private LocalDate snoozeUntil;

    /** 안함(C-10)으로 참고함에 온 항목 */
    @ColumnDefault("false")
    @Column(nullable = false)
    private boolean dismissed;

    /** 상태(실천·완료·참고)가 마지막으로 바뀐 시각. 이 컬럼이 생기기 전 기록은 classifiedAt 사용 */
    @Column(name = "status_changed_at")
    private Instant statusChangedAt;

    /** 참고함 폴더 */
    @Column(name = "folder_id")
    private Long folderId;

    public ScreenshotClassification(User user, String fileKey, Classification classification) {
        this.user = user;
        this.fileKey = fileKey;
        this.classification = classification;
        this.classifiedAt = Instant.now();
        this.statusChangedAt = this.classifiedAt;
    }

    public void changeClassification(Classification classification) {
        if (this.classification != classification) {
            // 실천·참고를 다시 고르면 실천함 진행 상태는 처음부터
            this.completedAt = null;
            this.snoozeUntil = null;
            this.dismissed = false;
            this.statusChangedAt = Instant.now();
        }
        this.classification = classification;
    }

    public Instant getStatusChangedAt() {
        return statusChangedAt != null ? statusChangedAt : classifiedAt;
    }

    public void complete(Instant now) {
        this.completedAt = now;
        this.snoozeUntil = null;
        this.statusChangedAt = now;
    }

    public void snooze(LocalDate until) {
        this.snoozeUntil = until;
    }

    public void dismiss(Instant now) {
        this.classification = Classification.REFERENCE;
        this.dismissed = true;
        this.completedAt = null;
        this.snoozeUntil = null;
        this.statusChangedAt = now;
    }

    public void reopen(Instant now) {
        this.completedAt = null;
        this.statusChangedAt = now;
    }

    public void moveToFolder(Long folderId) {
        this.folderId = folderId;
    }

    /** 되돌리기(C-11)용 상태 스냅샷 */
    public StateSnapshot snapshot() {
        return new StateSnapshot(classification, completedAt, snoozeUntil, dismissed, statusChangedAt);
    }

    public void restore(StateSnapshot snapshot) {
        this.classification = snapshot.classification();
        this.completedAt = snapshot.completedAt();
        this.snoozeUntil = snapshot.snoozeUntil();
        this.dismissed = snapshot.dismissed();
        this.statusChangedAt = snapshot.statusChangedAt();
    }

    public record StateSnapshot(Classification classification, Instant completedAt, LocalDate snoozeUntil,
                                boolean dismissed, Instant statusChangedAt) {
    }
}
