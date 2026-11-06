package com.nervatech.nerva_build.dto;

import jakarta.validation.constraints.NotBlank;

public record OrderStatusUpdateRequestDTO(
        @NotBlank(message = "Status alvo e obrigatorio")
        String status
) {
}
