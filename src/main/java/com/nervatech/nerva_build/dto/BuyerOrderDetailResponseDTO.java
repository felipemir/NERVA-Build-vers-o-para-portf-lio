package com.nervatech.nerva_build.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record BuyerOrderDetailResponseDTO(
        Long id,
        String orderNumber,
        Long storeId,
        String status,
        String paymentStatus,
        BigDecimal totalAmount,
        List<OrderItemResponseDTO> items,
        AdminOrderAddressResponseDTO address,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
