package com.nervatech.nerva_build.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record BuyerOrderSummaryResponseDTO(
        Long id,
        String orderNumber,
        Long storeId,
        String status,
        String paymentStatus,
        BigDecimal totalAmount,
        int itemCount,
        LocalDateTime createdAt
) {
}
