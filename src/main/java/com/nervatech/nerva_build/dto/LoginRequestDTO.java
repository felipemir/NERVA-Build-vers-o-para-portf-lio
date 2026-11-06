package com.nervatech.nerva_build.dto;

public record LoginRequestDTO(
        String email,
        String password,
        Long storeId
) {}