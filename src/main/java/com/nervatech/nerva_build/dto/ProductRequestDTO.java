package com.nervatech.nerva_build.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ProductRequestDTO(
        @NotBlank(message = "Nome do produto e obrigatorio")
        @Size(max = 200, message = "Nome do produto deve ter no maximo 200 caracteres")
        String name,
        @Size(max = 2000, message = "Descricao deve ter no maximo 2000 caracteres")
        String description,
        @Size(max = 120, message = "Marca deve ter no maximo 120 caracteres")
        String brand,
        @NotBlank(message = "Unidade de venda e obrigatoria")
        @Size(max = 20, message = "Unidade de venda deve ter no maximo 20 caracteres")
        String unit,
        @Size(max = 50, message = "SKU deve ter no maximo 50 caracteres")
        String sku,
        @NotNull(message = "Preco e obrigatorio")
        @DecimalMin(value = "0.0", inclusive = true, message = "Preco deve ser maior ou igual a zero")
        BigDecimal price,
        @PositiveOrZero(message = "Estoque deve ser maior ou igual a zero")
        BigDecimal stockQuantity,
        @DecimalMin(value = "0.0", inclusive = true, message = "Peso deve ser maior ou igual a zero")
        BigDecimal weightKg,
        @DecimalMin(value = "0.0", inclusive = true, message = "Comprimento deve ser maior ou igual a zero")
        BigDecimal lengthCm,
        @DecimalMin(value = "0.0", inclusive = true, message = "Largura deve ser maior ou igual a zero")
        BigDecimal widthCm,
        @DecimalMin(value = "0.0", inclusive = true, message = "Altura deve ser maior ou igual a zero")
        BigDecimal heightCm,
        @DecimalMin(value = "0.0", inclusive = false, message = "Quantidade minima deve ser maior que zero")
        BigDecimal minimumOrderQuantity,
        @Size(max = 500, message = "URL da imagem deve ter no maximo 500 caracteres")
        String imageUrl,
        Long categoryId,
        Boolean deliveryAvailable,
        Boolean pickupAvailable,
        Boolean active
) {
}
