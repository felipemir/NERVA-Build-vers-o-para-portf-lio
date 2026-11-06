package com.nervatech.nerva_build.dto;

public record StoreOnboardingResponseDTO(
        Long storeId,
        String storeName,
        String subdomain,
        Boolean storeActive,
        Long ownerId,
        String ownerName,
        String ownerEmail,
        String ownerRole,
        Boolean ownerActive
) {
}
