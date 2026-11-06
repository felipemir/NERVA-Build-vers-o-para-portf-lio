package com.nervatech.nerva_build.repository;

import com.nervatech.nerva_build.domain.model.CustomerAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CustomerAccountRepository extends JpaRepository<CustomerAccount, Long> {
    Optional<CustomerAccount> findByIdAndActiveTrue(Long id);

    Optional<CustomerAccount> findByEmailIgnoreCaseAndActiveTrue(String email);

    Optional<CustomerAccount> findByDocumentAndActiveTrue(String document);

    Optional<CustomerAccount> findFirstByPhoneAndActiveTrueOrderByIdAsc(String phone);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByDocument(String document);

    boolean existsByDocumentAndIdNot(String document, Long id);
}
