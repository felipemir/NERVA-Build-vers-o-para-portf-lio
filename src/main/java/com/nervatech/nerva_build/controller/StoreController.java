package com.nervatech.nerva_build.controller;

import com.nervatech.nerva_build.domain.model.Store;
import com.nervatech.nerva_build.dto.StoreOnboardingRequestDTO;
import com.nervatech.nerva_build.dto.StoreOnboardingResponseDTO;
import com.nervatech.nerva_build.security.AppUserDetails;
import com.nervatech.nerva_build.service.AccessControlService;
import com.nervatech.nerva_build.service.StoreService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/stores")
public class StoreController {

    private final StoreService storeService;
    private final AccessControlService accessControlService;
    private final boolean publicOnboardingEnabled;

    public StoreController(
            StoreService storeService,
            AccessControlService accessControlService,
            @Value("${app.onboarding.public-enabled:true}") boolean publicOnboardingEnabled
    ) {
        this.storeService = storeService;
        this.accessControlService = accessControlService;
        this.publicOnboardingEnabled = publicOnboardingEnabled;
    }

    // Rota para CRIAR uma loja (POST)
    @PostMapping
    public ResponseEntity<Store> createStore(
            @RequestBody Store store,
            @AuthenticationPrincipal AppUserDetails currentUser
    ) {
        accessControlService.requireStoreManagement(currentUser);
        Store newStore = storeService.createStore(store);
        return ResponseEntity.status(HttpStatus.CREATED).body(newStore);
    }

    @PostMapping("/register")
    public ResponseEntity<StoreOnboardingResponseDTO> registerStore(
            @Valid @RequestBody StoreOnboardingRequestDTO request
    ) {
        if (!publicOnboardingEnabled) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Recurso nao encontrado");
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(storeService.registerStore(request));
    }

    // Rota para LISTAR todas as lojas (GET)
    @GetMapping
    public ResponseEntity<List<Store>> getAllStores(@AuthenticationPrincipal AppUserDetails currentUser) {
        accessControlService.requireStoreManagement(currentUser);
        List<Store> stores = storeService.findAll();
        return ResponseEntity.ok(stores);
    }
}
