package com.nervatech.nerva_build.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record AdminOrderDetailResponseDTO(
        Long id,
        String orderNumber,
        Long storeId,
        String status,
        String paymentStatus,
        String paymentMethod,
        BigDecimal totalAmount,
        String notes,
        AdminOrderBuyerResponseDTO buyer,
        AdminOrderAddressResponseDTO address,
        List<AdminOrderItemSummaryResponseDTO> items,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        Long userId,
        Long updatedByUserId
) {
}
