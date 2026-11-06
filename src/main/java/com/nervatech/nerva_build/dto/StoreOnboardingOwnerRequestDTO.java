package com.nervatech.nerva_build.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record StoreOnboardingOwnerRequestDTO(
        @NotBlank(message = "Nome do dono e obrigatorio")
        @Size(max = 150, message = "Nome do dono deve ter no maximo 150 caracteres")
        String name,
        @NotBlank(message = "Email do dono e obrigatorio")
        @Email(message = "Email do dono invalido")
        @Size(max = 150, message = "Email do dono deve ter no maximo 150 caracteres")
        String email,
        @NotBlank(message = "Senha do dono e obrigatoria")
        @Size(min = 6, max = 100, message = "Senha do dono deve ter entre 6 e 100 caracteres")
        String password
) {
}
