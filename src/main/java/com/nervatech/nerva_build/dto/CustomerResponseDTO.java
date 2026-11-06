package com.nervatech.nerva_build.dto;

public record CustomerResponseDTO(
        Long id,
        Long storeId,
        String name,
        String document,
        String email,
        String phone,
        String zipCode,
        String street,
        String number,
        String neighborhood,
        String city,
        String state,
        String personType,
        Boolean active
) {}