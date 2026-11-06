package com.nervatech.nerva_build.controller;

import com.nervatech.nerva_build.dto.ProductPublicResponseDTO;
import com.nervatech.nerva_build.service.ProductService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/marketplace")
public class MarketplaceController {

    private final ProductService productService;

    public MarketplaceController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/products")
    public ResponseEntity<List<ProductPublicResponseDTO>> listProducts(
            @RequestParam(value = "q", required = false) String term
    ) {
        return ResponseEntity.ok(productService.findMarketplaceCatalog(term));
    }
}
