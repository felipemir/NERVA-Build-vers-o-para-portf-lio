package com.nervatech.nerva_build.dto;

public record AdminOrderAddressResponseDTO(
        String zipCode,
        String street,
        String number,
        String neighborhood,
        String city,
        String state
) {
}
