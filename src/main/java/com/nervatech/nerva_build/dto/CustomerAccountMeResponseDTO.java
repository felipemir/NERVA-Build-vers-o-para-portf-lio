package com.nervatech.nerva_build.dto;

public record CustomerAccountMeResponseDTO(
        Long customerId,
        String name,
        String email,
        String document,
        String phone,
        String personType
) {
}
