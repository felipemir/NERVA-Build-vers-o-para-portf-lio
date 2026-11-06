package com.nervatech.nerva_build.dto;

import java.math.BigDecimal;

public record OrderItemResponseDTO(
        Long productId,
        String sku,
        String name,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal subtotal
) {
}
