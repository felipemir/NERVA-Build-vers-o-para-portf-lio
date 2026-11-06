package com.nervatech.nerva_build.dto;

public record CustomerAccountAuthResponseDTO(
        String accessToken,
        String tokenType,
        long expiresInMs,
        Long customerId,
        String name,
        String email,
        String personType
) {
}
