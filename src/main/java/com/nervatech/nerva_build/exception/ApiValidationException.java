package com.nervatech.nerva_build.exception;

import com.nervatech.nerva_build.dto.ApiFieldErrorDTO;
import lombok.Getter;

import java.util.List;

@Getter
public class ApiValidationException extends RuntimeException {

    private final List<ApiFieldErrorDTO> fieldErrors;

    public ApiValidationException(String message, List<ApiFieldErrorDTO> fieldErrors) {
        super(message);
        this.fieldErrors = fieldErrors == null ? List.of() : fieldErrors;
    }
}
