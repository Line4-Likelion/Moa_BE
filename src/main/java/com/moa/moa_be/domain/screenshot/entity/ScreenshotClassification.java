package com.moa.moa_be.domain.screenshot.entity;

import com.moa.moa_be.domain.user.entity.User;
import jakarta.persistence.*;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

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

    public ScreenshotClassification(User user, String fileKey, Classification classification) {
        this.user = user;
        this.fileKey = fileKey;
        this.classification = classification;
        this.classifiedAt = Instant.now();
    }

    public void changeClassification(Classification classification) {
        this.classification = classification;
    }
}
