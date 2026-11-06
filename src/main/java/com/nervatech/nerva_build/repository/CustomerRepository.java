package com.nervatech.nerva_build.repository;

import com.nervatech.nerva_build.domain.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
    List<Customer> findByStoreIdOrderByNameAsc(Long storeId);

    Optional<Customer> findByIdAndStoreId(Long id, Long storeId);

    Optional<Customer> findByStoreIdAndDocument(Long storeId, String document);

    Optional<Customer> findFirstByStoreIdAndEmailIgnoreCaseOrderByIdAsc(Long storeId, String email);

    boolean existsByStoreIdAndDocument(Long storeId, String document);

    boolean existsByStoreIdAndDocumentAndIdNot(Long storeId, String document, Long id);

    @Query("""
            select c
            from Customer c
            where c.storeId = :storeId
              and (
                    :term is null
                    or lower(c.name) like lower(concat('%', :term, '%'))
                    or lower(coalesce(c.document, '')) like lower(concat('%', :term, '%'))
                    or lower(coalesce(c.email, '')) like lower(concat('%', :term, '%'))
              )
            order by c.name asc
            """)
    List<Customer> searchByStoreId(@Param("storeId") Long storeId, @Param("term") String term);
}
