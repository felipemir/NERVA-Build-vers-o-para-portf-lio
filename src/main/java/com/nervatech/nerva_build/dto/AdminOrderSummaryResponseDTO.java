package com.nervatech.nerva_build.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record AdminOrderSummaryResponseDTO(
        Long id,
        String orderNumber,
        String status,
        String paymentStatus,
        BigDecimal totalAmount,
        Long customerId,
        Long customerAccountId,
        String buyerName,
        String buyerEmail,
        String buyerPhone,
        Integer itemsCount,
        List<AdminOrderItemSummaryResponseDTO> items,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
