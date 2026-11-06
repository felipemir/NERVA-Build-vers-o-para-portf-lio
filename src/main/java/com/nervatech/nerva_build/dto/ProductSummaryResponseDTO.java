package com.nervatech.nerva_build.dto;

import java.math.BigDecimal;

public record ProductSummaryResponseDTO(
        Long id,
        String name,
        String description,
        String brand,
        String unit,
        String sku,
        BigDecimal price,
        BigDecimal stockQuantity,
        BigDecimal weightKg,
        BigDecimal lengthCm,
        BigDecimal widthCm,
        BigDecimal heightCm,
        BigDecimal minimumOrderQuantity,
        String imageUrl,
        Long categoryId,
        Boolean deliveryAvailable,
        Boolean pickupAvailable,
        Boolean active
) {
}
