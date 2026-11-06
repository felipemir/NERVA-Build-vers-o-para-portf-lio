package com.nervatech.nerva_build.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record StoreOnboardingRequestDTO(
        @NotBlank(message = "Nome da loja e obrigatorio")
        @Size(max = 150, message = "Nome da loja deve ter no maximo 150 caracteres")
        String name,
        @NotBlank(message = "Subdominio da loja e obrigatorio")
        @Size(min = 3, max = 63, message = "Subdominio deve ter entre 3 e 63 caracteres")
        @Pattern(regexp = "^[a-zA-Z0-9][a-zA-Z0-9-]*[a-zA-Z0-9]$", message = "Subdominio deve conter apenas letras, numeros e hifen, sem hifen nas pontas")
        String subdomain,
        @NotNull(message = "Dono da loja e obrigatorio")
        @Valid
        StoreOnboardingOwnerRequestDTO owner
) {
}
