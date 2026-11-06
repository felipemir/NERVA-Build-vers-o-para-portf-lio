package com.nervatech.nerva_build.service;

import com.nervatech.nerva_build.domain.model.AppUser;
import com.nervatech.nerva_build.domain.model.Store;
import com.nervatech.nerva_build.dto.StoreOnboardingRequestDTO;
import com.nervatech.nerva_build.dto.StoreOnboardingResponseDTO;
import com.nervatech.nerva_build.repository.AppUserRepository;
import com.nervatech.nerva_build.repository.StoreRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;

@Service
public class StoreService {

    private final StoreRepository storeRepository;
    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;

    public StoreService(
            StoreRepository storeRepository,
            AppUserRepository appUserRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.storeRepository = storeRepository;
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public Store createStore(Store store) {
        return storeRepository.save(store);
    }

    @Transactional
    public StoreOnboardingResponseDTO registerStore(StoreOnboardingRequestDTO request) {
        String storeName = request.name().trim();
        String subdomain = request.subdomain().trim().toLowerCase(Locale.ROOT);
        String ownerName = request.owner().name().trim();
        String ownerEmail = request.owner().email().trim().toLowerCase(Locale.ROOT);

        if (storeRepository.existsBySubdomain(subdomain)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ja existe loja com este subdominio");
        }

        Store store = new Store();
        store.setName(storeName);
        store.setSubdomain(subdomain);
        store.setActive(true);
        store.setMarketplaceVisible(false);
        Store savedStore = storeRepository.save(store);

        if (appUserRepository.existsByEmailAndStoreId(ownerEmail, savedStore.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ja existe usuario com este email nessa loja");
        }

        AppUser owner = new AppUser();
        owner.setStoreId(savedStore.getId());
        owner.setName(ownerName);
        owner.setEmail(ownerEmail);
        owner.setPassword(passwordEncoder.encode(request.owner().password()));
        owner.setRole("STORE_OWNER");
        owner.setActive(true);
        AppUser savedOwner = appUserRepository.save(owner);

        return new StoreOnboardingResponseDTO(
                savedStore.getId(),
                savedStore.getName(),
                savedStore.getSubdomain(),
                savedStore.getActive(),
                savedOwner.getId(),
                savedOwner.getName(),
                savedOwner.getEmail(),
                savedOwner.getRole(),
                savedOwner.getActive()
        );
    }

    public List<Store> findAll() {
        return storeRepository.findAll();
    }

    public Store findByIdOrThrow(Long storeId) {
        return storeRepository.findById(storeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Loja nao encontrada"));
    }

    public Store getDefaultStoreOrThrow() {
        return storeRepository.findAll()
                .stream()
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Nenhuma loja cadastrada"));
    }
}
