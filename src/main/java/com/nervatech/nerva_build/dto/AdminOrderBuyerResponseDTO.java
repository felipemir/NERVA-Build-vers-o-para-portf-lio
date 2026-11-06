package com.nervatech.nerva_build.dto;

public record AdminOrderBuyerResponseDTO(
        Long customerId,
        Long customerAccountId,
        String name,
        String email,
        String phone,
        String document,
        String personType
) {
}
