package com.nervatech.nerva_build.repository;

import com.nervatech.nerva_build.domain.model.Store;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StoreRepository extends JpaRepository<Store, Long> {
    Optional<Store> findBySubdomainAndActiveTrue(String subdomain);
    boolean existsBySubdomain(String subdomain);
}
