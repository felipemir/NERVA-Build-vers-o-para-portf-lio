package com.nervatech.nerva_build.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CustomerAccountRegisterRequestDTO(
        @NotBlank(message = "Nome e obrigatorio")
        @Size(max = 150, message = "Nome deve ter no maximo 150 caracteres")
        String name,
        @NotBlank(message = "Email e obrigatorio")
        @Email(message = "Email invalido")
        @Size(max = 150, message = "Email deve ter no maximo 150 caracteres")
        String email,
        @NotBlank(message = "Senha e obrigatoria")
        @Size(min = 6, max = 100, message = "Senha deve ter entre 6 e 100 caracteres")
        String password,
        @Size(max = 30, message = "Documento deve ter no maximo 30 caracteres")
        String document,
        @Size(max = 30, message = "Telefone deve ter no maximo 30 caracteres")
        String phone,
        @NotBlank(message = "personType e obrigatorio")
        @Pattern(regexp = "^(?i)(PF|PJ)$", message = "personType deve ser PF ou PJ")
        String personType
) {
}
