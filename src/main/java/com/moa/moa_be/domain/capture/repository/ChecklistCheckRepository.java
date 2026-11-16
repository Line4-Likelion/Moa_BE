package com.moa.moa_be.domain.capture.repository;

import com.moa.moa_be.domain.capture.entity.ChecklistCheck;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChecklistCheckRepository extends JpaRepository<ChecklistCheck, Long> {

    List<ChecklistCheck> findAllByCaptureId(Long captureId);

    List<ChecklistCheck> findAllByCaptureIdIn(Collection<Long> captureIds);

    Optional<ChecklistCheck> findByCaptureIdAndItemNo(Long captureId, int itemNo);

    @Modifying(flushAutomatically = true)
    @Query("delete from ChecklistCheck c where c.capture.id = :captureId")
    void deleteAllByCaptureId(@Param("captureId") Long captureId);
}
