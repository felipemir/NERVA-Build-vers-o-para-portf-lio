package com.nervatech.nerva_build.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record PublicOrderCreateRequestDTO(
        @NotNull(message = "Comprador e obrigatorio")
        @Valid
        PublicOrderBuyerRequestDTO buyer,
        @NotEmpty(message = "Pedido deve conter pelo menos um item")
        @Size(max = 100, message = "Pedido deve conter no maximo 100 itens")
        List<@Valid PublicOrderItemRequestDTO> items,
        @Size(max = 2000, message = "Observacoes devem ter no maximo 2000 caracteres")
        String notes
) {
}
