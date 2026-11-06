package com.nervatech.nerva_build.dto;

public record LoginResponseDTO(
        String accessToken,
        String tokenType,
        long expiresInMs,
        Long userId,
        String name,
        String email,
        String role,
        Long storeId
) {}