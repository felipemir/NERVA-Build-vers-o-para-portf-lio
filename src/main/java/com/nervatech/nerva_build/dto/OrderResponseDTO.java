package com.nervatech.nerva_build.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResponseDTO(
        Long id,
        String orderNumber,
        Long storeId,
        Long customerId,
        Long customerAccountId,
        String status,
        String paymentStatus,
        BigDecimal totalAmount,
        List<OrderItemResponseDTO> items,
        LocalDateTime createdAt
) {
}
