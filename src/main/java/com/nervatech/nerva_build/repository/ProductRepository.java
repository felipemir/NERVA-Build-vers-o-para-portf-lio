package com.nervatech.nerva_build.repository;

import com.nervatech.nerva_build.domain.model.Product;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {
    Optional<Product> findByIdAndStore_Id(Long id, Long storeId);
    Optional<Product> findByIdAndStore_IdAndActiveTrue(Long id, Long storeId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Product> findWithLockByIdAndStore_Id(Long id, Long storeId);

    List<Product> findByStore_IdAndIdInAndActiveTrue(Long storeId, List<Long> ids);
    boolean existsByStore_IdAndSku(Long storeId, String sku);
    boolean existsByStore_IdAndSkuAndIdNot(Long storeId, String sku, Long id);

    @Query(value = """
            SELECT *
            FROM product p
            WHERE p.store_id = :storeId
              AND p.active = true
              AND (:categoryId IS NULL OR p.category_id = :categoryId)
              AND (
                    :term IS NULL
                    OR p.name ILIKE '%' || :term || '%'
                    OR p.description ILIKE '%' || :term || '%'
                    OR p.sku ILIKE '%' || :term || '%'
              )
            ORDER BY p.name ASC
            """, nativeQuery = true)
    List<Product> searchActiveCatalog(
            @Param("storeId") Long storeId,
            @Param("term") String term,
            @Param("categoryId") Long categoryId
    );

    @Query(value = """
            SELECT p.*
            FROM product p
            JOIN store s ON s.id = p.store_id
            WHERE p.active = true
              AND s.active = true
              AND s.marketplace_visible = true
              AND (
                    :term IS NULL
                    OR p.name ILIKE '%' || :term || '%'
                    OR p.description ILIKE '%' || :term || '%'
                    OR p.sku ILIKE '%' || :term || '%'
              )
            ORDER BY s.name ASC, p.name ASC
            """, nativeQuery = true)
    List<Product> searchActiveMarketplace(@Param("term") String term);

    @Query(value = """
            SELECT *
            FROM product p
            WHERE p.store_id = :storeId
              AND (:categoryId IS NULL OR p.category_id = :categoryId)
              AND (
                    :term IS NULL
                    OR p.name ILIKE '%' || :term || '%'
                    OR p.description ILIKE '%' || :term || '%'
                    OR p.sku ILIKE '%' || :term || '%'
              )
            ORDER BY p.name ASC
            """, nativeQuery = true)
    List<Product> searchForAdmin(
            @Param("storeId") Long storeId,
            @Param("term") String term,
            @Param("categoryId") Long categoryId
    );
}
