package com.nervatech.nerva_build.controller;

import com.nervatech.nerva_build.dto.CustomerAccountAuthResponseDTO;
import com.nervatech.nerva_build.dto.CustomerAccountLoginRequestDTO;
import com.nervatech.nerva_build.dto.CustomerAccountMeResponseDTO;
import com.nervatech.nerva_build.dto.CustomerAccountRegisterRequestDTO;
import com.nervatech.nerva_build.security.CustomerAccountDetails;
import com.nervatech.nerva_build.service.CustomerAccountService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/customer-auth")
public class CustomerAccountAuthController {

    private final CustomerAccountService customerAccountService;

    public CustomerAccountAuthController(CustomerAccountService customerAccountService) {
        this.customerAccountService = customerAccountService;
    }

    @PostMapping("/register")
    public ResponseEntity<CustomerAccountAuthResponseDTO> register(
            @Valid @RequestBody CustomerAccountRegisterRequestDTO request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(customerAccountService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<CustomerAccountAuthResponseDTO> login(
            @Valid @RequestBody CustomerAccountLoginRequestDTO request
    ) {
        return ResponseEntity.ok(customerAccountService.login(request));
    }

    @GetMapping("/me")
    public ResponseEntity<CustomerAccountMeResponseDTO> me(
            @AuthenticationPrincipal CustomerAccountDetails currentCustomer
    ) {
        return ResponseEntity.ok(customerAccountService.me(currentCustomer));
    }
}
