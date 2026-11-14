package com.moa.moa_be.domain.screenshot.repository;

import com.moa.moa_be.domain.screenshot.entity.ScreenshotClassification;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScreenshotClassificationRepository extends JpaRepository<ScreenshotClassification, Long> {
    List<ScreenshotClassification> findByUserIdOrderByIdDesc(Long userId);
    Optional<ScreenshotClassification> findByIdAndUserId(Long id, Long userId);
    boolean existsByUserIdAndFileKeyIn(Long userId, Collection<String> fileKeys);
    void deleteByUserId(Long userId);
}
