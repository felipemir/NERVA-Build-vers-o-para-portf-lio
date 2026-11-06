package com.nervatech.nerva_build.dto;

public record RegisterRequestDTO(
        String name,
        String email,
        String password,
        Long storeId,
        String role
) {}