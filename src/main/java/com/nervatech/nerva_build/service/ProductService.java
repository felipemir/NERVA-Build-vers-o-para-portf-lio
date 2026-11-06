package com.nervatech.nerva_build.service;

import com.nervatech.nerva_build.domain.model.Product;
import com.nervatech.nerva_build.domain.model.Store;
import com.nervatech.nerva_build.dto.ProductPublicResponseDTO;
import com.nervatech.nerva_build.dto.ProductRequestDTO;
import com.nervatech.nerva_build.dto.ProductResponseDTO;
import com.nervatech.nerva_build.dto.ProductSummaryResponseDTO;
import com.nervatech.nerva_build.repository.ProductRepository;
import com.nervatech.nerva_build.service.StoreService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryService categoryService;
    private final StoreService storeService;

    public ProductService(ProductRepository productRepository, CategoryService categoryService, StoreService storeService) {
        this.productRepository = productRepository;
        this.categoryService = categoryService;
        this.storeService = storeService;
    }

    @Transactional
    public ProductResponseDTO createProduct(Long storeId, ProductRequestDTO request) {
        Store store = storeService.findByIdOrThrow(storeId);
        validateSkuUniqueness(store.getId(), request.sku(), null);
        categoryService.ensureCategoryBelongsToStore(store.getId(), request.categoryId());

        Product product = new Product();
        product.setStore(store);
        applyChanges(product, request);

        return toResponse(productRepository.save(product));
    }

    @Transactional(readOnly = true)
    public List<ProductSummaryResponseDTO> findCatalog(Long storeId, String term, Long categoryId) {
        return productRepository.searchActiveCatalog(storeId, normalizeTerm(term), categoryId)
                .stream()
                .map(this::toSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProductPublicResponseDTO findCatalogItem(Long storeId, Long productId) {
        return productRepository.findByIdAndStore_IdAndActiveTrue(productId, storeId)
                .map(this::toPublicResponse)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Produto nao encontrado"));
    }

    @Transactional(readOnly = true)
    public List<ProductPublicResponseDTO> findMarketplaceCatalog(String term) {
        return productRepository.searchActiveMarketplace(normalizeTerm(term))
                .stream()
                .map(this::toPublicResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProductResponseDTO> findAllByStore(Long storeId, String term, Long categoryId) {
        return productRepository.searchForAdmin(storeId, normalizeTerm(term), categoryId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProductResponseDTO findByIdForStore(Long storeId, Long productId) {
        return toResponse(findEntity(storeId, productId));
    }

    @Transactional
    public ProductResponseDTO updateProduct(Long storeId, Long productId, ProductRequestDTO request) {
        Product product = findEntity(storeId, productId);
        validateSkuUniqueness(storeId, request.sku(), productId);
        categoryService.ensureCategoryBelongsToStore(storeId, request.categoryId());

        applyChanges(product, request);
        return toResponse(productRepository.save(product));
    }

    @Transactional
    public void deactivate(Long storeId, Long productId) {
        Product product = findEntity(storeId, productId);
        product.setActive(false);
        productRepository.save(product);
    }

    private Product findEntity(Long storeId, Long productId) {
        return productRepository.findByIdAndStore_Id(productId, storeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Produto nao encontrado"));
    }

    private void applyChanges(Product product, ProductRequestDTO request) {
        product.setName(request.name().trim());
        product.setDescription(trimToNull(request.description()));
        product.setBrand(trimToNull(request.brand()));
        product.setUnit(request.unit().trim().toUpperCase());
        product.setSku(trimToNull(request.sku()));
        product.setPrice(request.price());
        product.setStockQuantity(request.stockQuantity() == null ? BigDecimal.ZERO : request.stockQuantity());
        product.setWeightKg(request.weightKg());
        product.setLengthCm(request.lengthCm());
        product.setWidthCm(request.widthCm());
        product.setHeightCm(request.heightCm());
        product.setMinimumOrderQuantity(request.minimumOrderQuantity() == null ? BigDecimal.ONE : request.minimumOrderQuantity());
        product.setImageUrl(trimToNull(request.imageUrl()));
        product.setCategoryId(request.categoryId());
        product.setDeliveryAvailable(request.deliveryAvailable() == null ? Boolean.TRUE : request.deliveryAvailable());
        product.setPickupAvailable(request.pickupAvailable() == null ? Boolean.TRUE : request.pickupAvailable());
        product.setActive(request.active() == null ? Boolean.TRUE : request.active());
    }

    private void validateSkuUniqueness(Long storeId, String sku, Long currentProductId) {
        String normalizedSku = trimToNull(sku);
        if (normalizedSku == null) {
            return;
        }

        boolean exists = currentProductId == null
                ? productRepository.existsByStore_IdAndSku(storeId, normalizedSku)
                : productRepository.existsByStore_IdAndSkuAndIdNot(storeId, normalizedSku, currentProductId);
        if (exists) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ja existe produto com este SKU na loja");
        }
    }

    private String normalizeTerm(String term) {
        return trimToNull(term);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private ProductResponseDTO toResponse(Product product) {
        return new ProductResponseDTO(
                product.getId(),
                product.getStore().getId(),
                product.getName(),
                product.getDescription(),
                product.getBrand(),
                product.getUnit(),
                product.getSku(),
                product.getPrice(),
                product.getStockQuantity(),
                product.getWeightKg(),
                product.getLengthCm(),
                product.getWidthCm(),
                product.getHeightCm(),
                product.getMinimumOrderQuantity(),
                product.getImageUrl(),
                product.getCategoryId(),
                product.getDeliveryAvailable(),
                product.getPickupAvailable(),
                product.getActive()
        );
    }

    private ProductPublicResponseDTO toPublicResponse(Product product) {
        return new ProductPublicResponseDTO(
                product.getId(),
                product.getStore().getId(),
                product.getName(),
                product.getDescription(),
                product.getBrand(),
                product.getUnit(),
                product.getPrice(),
                product.getStockQuantity(),
                product.getWeightKg(),
                product.getLengthCm(),
                product.getWidthCm(),
                product.getHeightCm(),
                product.getMinimumOrderQuantity(),
                product.getImageUrl(),
                product.getCategoryId(),
                product.getDeliveryAvailable(),
                product.getPickupAvailable()
        );
    }

    private ProductSummaryResponseDTO toSummary(Product product) {
        return new ProductSummaryResponseDTO(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getBrand(),
                product.getUnit(),
                product.getSku(),
                product.getPrice(),
                product.getStockQuantity(),
                product.getWeightKg(),
                product.getLengthCm(),
                product.getWidthCm(),
                product.getHeightCm(),
                product.getMinimumOrderQuantity(),
                product.getImageUrl(),
                product.getCategoryId(),
                product.getDeliveryAvailable(),
                product.getPickupAvailable(),
                product.getActive()
        );
    }
}
