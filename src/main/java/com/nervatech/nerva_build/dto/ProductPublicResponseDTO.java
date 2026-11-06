package com.nervatech.nerva_build.dto;

import java.math.BigDecimal;

public record ProductPublicResponseDTO(
        Long id,
        Long storeId,
        String name,
        String description,
        String brand,
        String unit,
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
        Boolean pickupAvailable
) {
}
