package com.nervatech.nerva_build.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CustomerAccountLoginRequestDTO(
        @NotBlank(message = "Email e obrigatorio")
        @Email(message = "Email invalido")
        @Size(max = 150, message = "Email deve ter no maximo 150 caracteres")
        String email,
        @NotBlank(message = "Senha e obrigatoria")
        @Size(max = 100, message = "Senha deve ter no maximo 100 caracteres")
        String password
) {
}
