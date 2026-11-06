package com.nervatech.nerva_build.service;

import com.nervatech.nerva_build.domain.model.AppUser;
import com.nervatech.nerva_build.repository.AppUserRepository;
import com.nervatech.nerva_build.security.AppUserDetails;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AppUserAuthService {

    private final AppUserRepository appUserRepository;

    public AppUserAuthService(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    @Transactional(readOnly = true)
    public AppUserDetails loadById(Long id) {
        AppUser user = appUserRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuário inválido"));
        return new AppUserDetails(user);
    }

    @Transactional(readOnly = true)
    public AppUser findByEmailAndStoreOrThrow(String email, Long storeId) {
        return appUserRepository.findByEmailAndStoreIdAndActiveTrue(email, storeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciais inválidas"));
    }

    @Transactional
    public AppUser save(AppUser user) {
        return appUserRepository.save(user);
    }
}