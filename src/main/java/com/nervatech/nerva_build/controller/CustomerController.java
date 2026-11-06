package com.nervatech.nerva_build.controller;

import com.nervatech.nerva_build.dto.CustomerRequestDTO;
import com.nervatech.nerva_build.dto.CustomerResponseDTO;
import com.nervatech.nerva_build.security.AppUserDetails;
import com.nervatech.nerva_build.service.CustomerService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @PostMapping
    public ResponseEntity<CustomerResponseDTO> create(
            @Valid @RequestBody CustomerRequestDTO request,
            @AuthenticationPrincipal AppUserDetails currentUser
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(customerService.create(request, currentUser));
    }

    @GetMapping
    public ResponseEntity<List<CustomerResponseDTO>> list(
            @RequestParam(value = "q", required = false) String term,
            @AuthenticationPrincipal AppUserDetails currentUser
    ) {
        return ResponseEntity.ok(customerService.search(term, currentUser));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CustomerResponseDTO> getById(
            @PathVariable Long id,
            @AuthenticationPrincipal AppUserDetails currentUser
    ) {
        return ResponseEntity.ok(customerService.getById(id, currentUser));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CustomerResponseDTO> update(
            @PathVariable Long id,
            @Valid @RequestBody CustomerRequestDTO request,
            @AuthenticationPrincipal AppUserDetails currentUser
    ) {
        return ResponseEntity.ok(customerService.update(id, request, currentUser));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivate(
            @PathVariable Long id,
            @AuthenticationPrincipal AppUserDetails currentUser
    ) {
        customerService.deactivate(id, currentUser);
        return ResponseEntity.noContent().build();
    }
}
