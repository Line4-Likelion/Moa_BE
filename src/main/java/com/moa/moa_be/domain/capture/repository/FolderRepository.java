package com.moa.moa_be.domain.capture.repository;

import com.moa.moa_be.domain.capture.entity.Folder;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FolderRepository extends JpaRepository<Folder, Long> {

    List<Folder> findAllByUserIdOrderByIdAsc(Long userId);

    Optional<Folder> findByIdAndUserId(Long id, Long userId);

    boolean existsByUserIdAndName(Long userId, String name);
}
