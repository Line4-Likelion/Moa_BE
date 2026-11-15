package com.moa.moa_be.domain.capture.repository;

import com.moa.moa_be.domain.capture.entity.FactOverride;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FactOverrideRepository extends JpaRepository<FactOverride, Long> {

    List<FactOverride> findAllByCaptureId(Long captureId);

    Optional<FactOverride> findByCaptureIdAndFactKey(Long captureId, String factKey);

    List<FactOverride> findAllByCaptureIdInAndFactKey(Collection<Long> captureIds, String factKey);

    @Modifying(flushAutomatically = true)
    @Query("delete from FactOverride f where f.capture.id = :captureId")
    void deleteAllByCaptureId(@Param("captureId") Long captureId);
}
