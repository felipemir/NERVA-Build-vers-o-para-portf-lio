package com.nervatech.nerva_build.controller;

import com.nervatech.nerva_build.dto.CategoryRequestDTO;
import com.nervatech.nerva_build.dto.CategoryResponseDTO;
import com.nervatech.nerva_build.security.AppUserDetails;
import com.nervatech.nerva_build.service.AccessControlService;
import com.nervatech.nerva_build.service.CategoryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService categoryService;
    private final AccessControlService accessControlService;

    public CategoryController(
            CategoryService categoryService,
            AccessControlService accessControlService
    ) {
        this.categoryService = categoryService;
        this.accessControlService = accessControlService;
    }

    @GetMapping
    public ResponseEntity<List<CategoryResponseDTO>> listActive(
            @RequestParam(value = "storeId", required = false) Long storeId
    ) {
        return ResponseEntity.ok(categoryService.listActiveByStore(resolveStoreId(storeId)));
    }

    @GetMapping("/slug/{slug}")
    public ResponseEntity<CategoryResponseDTO> getBySlug(
            @PathVariable String slug,
            @RequestParam(value = "storeId", required = false) Long storeId
    ) {
        return ResponseEntity.ok(categoryService.getBySlug(resolveStoreId(storeId), slug));
    }

    @GetMapping("/admin")
    public ResponseEntity<List<CategoryResponseDTO>> listAllForAdmin(
            @AuthenticationPrincipal AppUserDetails currentUser
    ) {
        Long storeId = accessControlService.requireCatalogStore(currentUser);
        return ResponseEntity.ok(categoryService.listAllByStore(storeId));
    }

    @GetMapping("/admin/{id}")
    public ResponseEntity<CategoryResponseDTO> getForAdmin(
            @PathVariable Long id,
            @AuthenticationPrincipal AppUserDetails currentUser
    ) {
        Long storeId = accessControlService.requireCatalogStore(currentUser);
        return ResponseEntity.ok(categoryService.getById(storeId, id));
    }

    @PostMapping
    public ResponseEntity<CategoryResponseDTO> create(
            @Valid @RequestBody CategoryRequestDTO request,
            @AuthenticationPrincipal AppUserDetails currentUser
    ) {
        Long storeId = accessControlService.requireCatalogStore(currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(categoryService.create(storeId, request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CategoryResponseDTO> update(
            @PathVariable Long id,
            @Valid @RequestBody CategoryRequestDTO request,
            @AuthenticationPrincipal AppUserDetails currentUser
    ) {
        Long storeId = accessControlService.requireCatalogStore(currentUser);
        return ResponseEntity.ok(categoryService.update(storeId, id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal AppUserDetails currentUser
    ) {
        Long storeId = accessControlService.requireCatalogStore(currentUser);
        categoryService.deactivate(storeId, id);
        return ResponseEntity.noContent().build();
    }

    private Long resolveStoreId(Long requestedStoreId) {
        if (requestedStoreId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "storeId e obrigatorio para catalogo publico de categorias");
        }
        return requestedStoreId;
    }
}
