package com.nervatech.nerva_build.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CustomerRequestDTO(
        @NotBlank(message = "Nome do cliente e obrigatorio")
        @Size(max = 150, message = "Nome do cliente deve ter no maximo 150 caracteres")
        String name,
        @Size(max = 30, message = "Documento deve ter no maximo 30 caracteres")
        String document,
        @Size(max = 150, message = "Email deve ter no maximo 150 caracteres")
        String email,
        @Size(max = 30, message = "Telefone deve ter no maximo 30 caracteres")
        String phone,
        @Size(max = 10, message = "CEP deve ter no maximo 10 caracteres")
        String zipCode,
        @Size(max = 150, message = "Rua deve ter no maximo 150 caracteres")
        String street,
        @Size(max = 20, message = "Numero deve ter no maximo 20 caracteres")
        String number,
        @Size(max = 100, message = "Bairro deve ter no maximo 100 caracteres")
        String neighborhood,
        @Size(max = 100, message = "Cidade deve ter no maximo 100 caracteres")
        String city,
        @Size(max = 2, message = "Estado deve ter no maximo 2 caracteres")
        String state,
        @Pattern(regexp = "^(?i)(PF|PJ)?$", message = "personType deve ser PF ou PJ")
        String personType,
        Boolean active
) {
}
