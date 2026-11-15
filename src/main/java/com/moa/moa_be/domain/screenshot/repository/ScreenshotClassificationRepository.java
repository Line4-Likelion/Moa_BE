package com.moa.moa_be.domain.screenshot.repository;

import com.moa.moa_be.domain.screenshot.entity.ScreenshotClassification;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

public interface ScreenshotClassificationRepository extends JpaRepository<ScreenshotClassification, Long> {
    List<ScreenshotClassification> findByUserIdOrderByIdDesc(Long userId);
    Optional<ScreenshotClassification> findByIdAndUserId(Long id, Long userId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from ScreenshotClassification s where s.id = :id and s.user.id = :userId")
    Optional<ScreenshotClassification> findOwnedForUpdate(@Param("id") Long id, @Param("userId") Long userId);
    boolean existsByUserIdAndFileKeyIn(Long userId, Collection<String> fileKeys);
    void deleteByUserId(Long userId);
}
