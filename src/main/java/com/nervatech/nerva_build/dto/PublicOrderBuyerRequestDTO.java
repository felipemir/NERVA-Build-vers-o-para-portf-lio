package com.nervatech.nerva_build.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PublicOrderBuyerRequestDTO(
        @NotBlank(message = "Nome do comprador e obrigatorio")
        @Size(max = 150, message = "Nome do comprador deve ter no maximo 150 caracteres")
        String name,
        @Size(max = 30, message = "Documento deve ter no maximo 30 caracteres")
        String document,
        @NotBlank(message = "Email do comprador e obrigatorio")
        @Email(message = "Email invalido")
        @Size(max = 150, message = "Email deve ter no maximo 150 caracteres")
        String email,
        @NotBlank(message = "Celular do comprador e obrigatorio")
        @Size(max = 30, message = "Telefone deve ter no maximo 30 caracteres")
        String phone,
        @Pattern(regexp = "^(?i)(PF|PJ)?$", message = "personType deve ser PF ou PJ")
        String personType,
        @NotBlank(message = "CEP do endereco e obrigatorio")
        @Size(max = 10, message = "CEP deve ter no maximo 10 caracteres")
        String zipCode,
        @NotBlank(message = "Rua do endereco e obrigatoria")
        @Size(max = 150, message = "Rua deve ter no maximo 150 caracteres")
        String street,
        @NotBlank(message = "Numero do endereco e obrigatorio")
        @Size(max = 20, message = "Numero deve ter no maximo 20 caracteres")
        String number,
        @NotBlank(message = "Bairro do endereco e obrigatorio")
        @Size(max = 100, message = "Bairro deve ter no maximo 100 caracteres")
        String neighborhood,
        @NotBlank(message = "Cidade do endereco e obrigatoria")
        @Size(max = 100, message = "Cidade deve ter no maximo 100 caracteres")
        String city,
        @NotBlank(message = "Estado do endereco e obrigatorio")
        @Size(max = 2, message = "Estado deve ter no maximo 2 caracteres")
        String state
) {
}
