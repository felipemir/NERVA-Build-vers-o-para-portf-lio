package com.nervatech.nerva_build.controller;

import com.nervatech.nerva_build.dto.ProductPublicResponseDTO;
import com.nervatech.nerva_build.dto.ProductRequestDTO;
import com.nervatech.nerva_build.dto.ProductResponseDTO;
import com.nervatech.nerva_build.dto.ProductSummaryResponseDTO;
import com.nervatech.nerva_build.security.AppUserDetails;
import com.nervatech.nerva_build.service.AccessControlService;
import com.nervatech.nerva_build.service.ProductService;
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
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;
    private final AccessControlService accessControlService;

    public ProductController(
            ProductService productService,
            AccessControlService accessControlService
    ) {
        this.productService = productService;
        this.accessControlService = accessControlService;
    }

    @PostMapping
    public ResponseEntity<ProductResponseDTO> createProduct(
            @Valid @RequestBody ProductRequestDTO dto,
            @AuthenticationPrincipal AppUserDetails currentUser
    ) {
        Long storeId = accessControlService.requireCatalogStore(currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.createProduct(storeId, dto));
    }

    @GetMapping
    public ResponseEntity<List<ProductSummaryResponseDTO>> getAllProducts(
            @RequestParam(value = "storeId", required = false) Long storeId,
            @RequestParam(value = "q", required = false) String term,
            @RequestParam(value = "categoryId", required = false) Long categoryId
    ) {
        return ResponseEntity.ok(productService.findCatalog(resolveStoreId(storeId), term, categoryId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductPublicResponseDTO> getProductById(
            @PathVariable Long id,
            @RequestParam(value = "storeId", required = false) Long storeId
    ) {
        return ResponseEntity.ok(productService.findCatalogItem(resolveStoreId(storeId), id));
    }

    @GetMapping("/admin")
    public ResponseEntity<List<ProductResponseDTO>> getProductsForAdmin(
            @RequestParam(value = "q", required = false) String term,
            @RequestParam(value = "categoryId", required = false) Long categoryId,
            @AuthenticationPrincipal AppUserDetails currentUser
    ) {
        Long storeId = accessControlService.requireCatalogStore(currentUser);
        return ResponseEntity.ok(productService.findAllByStore(storeId, term, categoryId));
    }

    @GetMapping("/admin/{id}")
    public ResponseEntity<ProductResponseDTO> getProductForAdmin(
            @PathVariable Long id,
            @AuthenticationPrincipal AppUserDetails currentUser
    ) {
        Long storeId = accessControlService.requireCatalogStore(currentUser);
        return ResponseEntity.ok(productService.findByIdForStore(storeId, id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductResponseDTO> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductRequestDTO dto,
            @AuthenticationPrincipal AppUserDetails currentUser
    ) {
        Long storeId = accessControlService.requireCatalogStore(currentUser);
        return ResponseEntity.ok(productService.updateProduct(storeId, id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(
            @PathVariable Long id,
            @AuthenticationPrincipal AppUserDetails currentUser
    ) {
        Long storeId = accessControlService.requireCatalogStore(currentUser);
        productService.deactivate(storeId, id);
        return ResponseEntity.noContent().build();
    }

    private Long resolveStoreId(Long requestedStoreId) {
        if (requestedStoreId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "storeId e obrigatorio para catalogo publico de loja");
        }
        return requestedStoreId;
    }
}
