package com.nervatech.nerva_build.dto;

public record MeResponseDTO(
        Long userId,
        String name,
        String email,
        String role,
        Long storeId
) {}