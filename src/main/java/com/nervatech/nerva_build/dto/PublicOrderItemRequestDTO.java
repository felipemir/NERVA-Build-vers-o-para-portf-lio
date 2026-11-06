package com.nervatech.nerva_build.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record PublicOrderItemRequestDTO(
        @NotNull(message = "Produto do item e obrigatorio")
        Long productId,
        @NotNull(message = "Quantidade do item e obrigatoria")
        @Positive(message = "Quantidade deve ser maior que zero")
        Integer quantity
) {
}
