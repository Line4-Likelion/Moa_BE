package com.moa.moa_be.domain.analysis.repository;

import com.moa.moa_be.domain.analysis.entity.AnalysisStatus;
import com.moa.moa_be.domain.analysis.entity.ScreenshotAnalysis;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScreenshotAnalysisRepository extends JpaRepository<ScreenshotAnalysis, Long> {

    Optional<ScreenshotAnalysis> findByCaptureId(Long captureId);

    List<ScreenshotAnalysis> findAllByIdInAndUserId(Collection<Long> ids, Long userId);

    /** 하루 분석 상한 계산용 */
    long countByUserIdAndRequestedAtGreaterThanEqual(Long userId, LocalDateTime from);

    /** 120초 넘게 끝나지 않은 작업 (타임아웃 처리용) */
    List<ScreenshotAnalysis> findAllByStatusInAndRequestedAtBefore(Collection<AnalysisStatus> statuses,
                                                                   LocalDateTime before);


    void deleteAllByUserId(Long userId);

    void deleteByCaptureId(Long captureId);
}
