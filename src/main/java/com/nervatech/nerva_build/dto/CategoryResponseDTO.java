package com.nervatech.nerva_build.dto;

public record CategoryResponseDTO(
        Long id,
        Long storeId,
        String name,
        String slug,
        String description,
        Boolean active
) {
}
