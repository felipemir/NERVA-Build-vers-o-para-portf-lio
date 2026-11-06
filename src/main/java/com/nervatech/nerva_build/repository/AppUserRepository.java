package com.nervatech.nerva_build.repository;

import com.nervatech.nerva_build.domain.model.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByEmailAndStoreIdAndActiveTrue(String email, Long storeId);
    Optional<AppUser> findByIdAndActiveTrue(Long id);
    boolean existsByEmailAndStoreId(String email, Long storeId);
}