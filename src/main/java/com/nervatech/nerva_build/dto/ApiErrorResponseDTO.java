package com.nervatech.nerva_build.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ApiErrorResponseDTO(
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        String path,
        List<ApiFieldErrorDTO> fieldErrors
) {
}
