package com.moa.moa_be.domain.analysis.repository;

import com.moa.moa_be.domain.analysis.entity.AnalysisStatus;
import com.moa.moa_be.domain.analysis.entity.ScreenshotAnalysis;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ScreenshotAnalysisRepository extends JpaRepository<ScreenshotAnalysis, Long> {

    Optional<ScreenshotAnalysis> findByCaptureId(Long captureId);

    List<ScreenshotAnalysis> findAllByIdInAndUserId(Collection<Long> ids, Long userId);

    /** 하루 분석 상한 계산용 */
    long countByUserIdAndRequestedAtGreaterThanEqual(Long userId, LocalDateTime from);

    /** 120초 넘게 끝나지 않은 작업 (타임아웃 처리용) */
    List<ScreenshotAnalysis> findAllByStatusInAndRequestedAtBefore(Collection<AnalysisStatus> statuses,
                                                                   LocalDateTime before);

    /** 카테고리별 분석 완료 항목 수 [categoryId, count] */
    @Query("""
            select a.categoryId, count(a) from ScreenshotAnalysis a
            where a.userId = :userId and a.status = com.moa.moa_be.domain.analysis.entity.AnalysisStatus.SUCCEEDED
              and a.categoryId is not null
            group by a.categoryId
            """)
    List<Object[]> countSucceededByCategory(@Param("userId") Long userId);

    void deleteAllByUserId(Long userId);

    void deleteByCaptureId(Long captureId);

    List<ScreenshotAnalysis> findAllByUserIdAndStatusAndLatitudeIsNotNullAndLongitudeIsNotNull(
            Long userId,
            AnalysisStatus status
    );
}
