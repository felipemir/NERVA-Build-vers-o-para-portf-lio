package com.nervatech.nerva_build.dto;

public record RegisterResponseDTO(
        Long userId,
        String name,
        String email,
        String role,
        Long storeId,
        Boolean active
) {}