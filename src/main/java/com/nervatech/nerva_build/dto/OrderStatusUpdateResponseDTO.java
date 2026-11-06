package com.nervatech.nerva_build.dto;

import java.time.LocalDateTime;

public record OrderStatusUpdateResponseDTO(
        Long orderId,
        String orderNumber,
        String status,
        Boolean stockCommitted,
        Long updatedByUserId,
        LocalDateTime updatedAt
) {
}
