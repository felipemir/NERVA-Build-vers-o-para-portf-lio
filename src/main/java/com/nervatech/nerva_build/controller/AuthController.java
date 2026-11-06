package com.nervatech.nerva_build.controller;

import com.nervatech.nerva_build.dto.LoginRequestDTO;
import com.nervatech.nerva_build.dto.LoginResponseDTO;
import com.nervatech.nerva_build.dto.MeResponseDTO;
import com.nervatech.nerva_build.dto.RegisterRequestDTO;
import com.nervatech.nerva_build.dto.RegisterResponseDTO;
import com.nervatech.nerva_build.security.AppUserDetails;
import com.nervatech.nerva_build.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@RequestBody LoginRequestDTO request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @GetMapping("/me")
    public ResponseEntity<MeResponseDTO> me(@AuthenticationPrincipal AppUserDetails currentUser) {
        if (currentUser == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Não autenticado");
        }
        return ResponseEntity.ok(authService.me(currentUser));
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterResponseDTO> register(
            @RequestBody RegisterRequestDTO request,
            @AuthenticationPrincipal AppUserDetails currentUser
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request, currentUser));
    }
}