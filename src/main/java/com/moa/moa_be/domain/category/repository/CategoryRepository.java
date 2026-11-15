package com.moa.moa_be.domain.category.repository;

import com.moa.moa_be.domain.category.entity.Category;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findAllByUserId(Long userId);

    boolean existsByUserIdAndName(Long userId, String name);

    Optional<Category> findByIdAndUserId(Long id, Long userId);

    Optional<Category> findByUserIdAndName(Long userId, String name);

    void deleteAllByUserId(Long userId);
}
