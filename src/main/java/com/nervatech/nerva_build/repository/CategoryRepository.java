package com.nervatech.nerva_build.repository;

import com.nervatech.nerva_build.domain.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findByStoreIdAndActiveTrueOrderByNameAsc(Long storeId);
    List<Category> findByStoreIdOrderByNameAsc(Long storeId);
    Optional<Category> findByIdAndStoreId(Long id, Long storeId);
    Optional<Category> findByStoreIdAndSlugAndActiveTrue(Long storeId, String slug);
    boolean existsByStoreIdAndSlug(Long storeId, String slug);
    boolean existsByStoreIdAndSlugAndIdNot(Long storeId, String slug, Long id);
    Optional<Category> findByStoreIdAndSlug(Long storeId, String slug);
}
