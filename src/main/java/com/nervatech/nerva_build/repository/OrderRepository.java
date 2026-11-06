package com.nervatech.nerva_build.repository;

import com.nervatech.nerva_build.domain.model.Order;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    @EntityGraph(attributePaths = {"items", "customer", "customerAccount"})
    Optional<Order> findByIdAndStoreId(Long id, Long storeId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"items", "customer", "customerAccount"})
    Optional<Order> findWithLockByIdAndStoreId(Long id, Long storeId);

    @EntityGraph(attributePaths = {"items"})
    List<Order> findByCustomerAccountIdOrderByCreatedAtDescIdDesc(Long customerAccountId);

    @EntityGraph(attributePaths = {"items"})
    Optional<Order> findByIdAndCustomerAccountId(Long id, Long customerAccountId);

    @EntityGraph(attributePaths = {"items", "customer", "customerAccount"})
    @Query("""
            select distinct o
            from Order o
            left join o.customer c
            left join o.customerAccount ca
            where o.storeId = :storeId
              and (:status = '' or o.status = :status)
              and o.createdAt >= :createdFrom
              and o.createdAt < :createdTo
              and (
                    :buyerTerm = ''
                    or lower(coalesce(c.name, '')) like lower(concat('%', :buyerTerm, '%'))
                    or lower(coalesce(c.email, '')) like lower(concat('%', :buyerTerm, '%'))
                    or lower(coalesce(c.phone, '')) like lower(concat('%', :buyerTerm, '%'))
                    or lower(coalesce(c.document, '')) like lower(concat('%', :buyerTerm, '%'))
                    or lower(coalesce(ca.name, '')) like lower(concat('%', :buyerTerm, '%'))
                    or lower(coalesce(ca.email, '')) like lower(concat('%', :buyerTerm, '%'))
                    or lower(coalesce(ca.phone, '')) like lower(concat('%', :buyerTerm, '%'))
                    or lower(coalesce(ca.document, '')) like lower(concat('%', :buyerTerm, '%'))
              )
            order by o.createdAt desc, o.id desc
            """)
    List<Order> searchAdminOrders(
            @Param("storeId") Long storeId,
            @Param("status") String status,
            @Param("buyerTerm") String buyerTerm,
            @Param("createdFrom") LocalDateTime createdFrom,
            @Param("createdTo") LocalDateTime createdTo
    );
}
