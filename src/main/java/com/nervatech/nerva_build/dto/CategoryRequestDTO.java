package com.nervatech.nerva_build.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoryRequestDTO(
        @NotBlank(message = "Nome da categoria e obrigatorio")
        @Size(max = 120, message = "Nome da categoria deve ter no maximo 120 caracteres")
        String name,
        @Size(max = 140, message = "Slug deve ter no maximo 140 caracteres")
        String slug,
        @Size(max = 1000, message = "Descricao deve ter no maximo 1000 caracteres")
        String description,
        Boolean active
) {
}
