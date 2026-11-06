package com.nervatech.nerva_build.service;

import com.nervatech.nerva_build.domain.model.CustomerAccount;
import com.nervatech.nerva_build.dto.CustomerAccountAuthResponseDTO;
import com.nervatech.nerva_build.dto.CustomerAccountLoginRequestDTO;
import com.nervatech.nerva_build.dto.CustomerAccountMeResponseDTO;
import com.nervatech.nerva_build.dto.CustomerAccountRegisterRequestDTO;
import com.nervatech.nerva_build.repository.CustomerAccountRepository;
import com.nervatech.nerva_build.security.CustomerAccountDetails;
import com.nervatech.nerva_build.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;

@Service
public class CustomerAccountService {

    private final CustomerAccountRepository customerAccountRepository;
    private final CustomerAccountAuthService customerAccountAuthService;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    public CustomerAccountService(
            CustomerAccountRepository customerAccountRepository,
            CustomerAccountAuthService customerAccountAuthService,
            JwtService jwtService,
            PasswordEncoder passwordEncoder
    ) {
        this.customerAccountRepository = customerAccountRepository;
        this.customerAccountAuthService = customerAccountAuthService;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public CustomerAccountAuthResponseDTO register(CustomerAccountRegisterRequestDTO request) {
        String normalizedEmail = normalizeEmail(request.email());
        String normalizedDocument = normalizeDocument(request.document());

        if (customerAccountRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ja existe cliente com este email");
        }

        if (normalizedDocument != null && customerAccountRepository.existsByDocument(normalizedDocument)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ja existe cliente com este documento");
        }

        CustomerAccount customerAccount = new CustomerAccount();
        customerAccount.setName(request.name().trim());
        customerAccount.setEmail(normalizedEmail);
        customerAccount.setPassword(passwordEncoder.encode(request.password()));
        customerAccount.setDocument(normalizedDocument);
        customerAccount.setPhone(trimToNull(request.phone()));
        customerAccount.setPersonType(request.personType().trim().toUpperCase(Locale.ROOT));
        customerAccount.setActive(true);

        CustomerAccount saved = customerAccountAuthService.save(customerAccount);
        return toAuthResponse(saved);
    }

    @Transactional(readOnly = true)
    public CustomerAccountAuthResponseDTO login(CustomerAccountLoginRequestDTO request) {
        String normalizedEmail = normalizeEmail(request.email());
        CustomerAccount customerAccount = customerAccountAuthService.findByEmailOrThrow(normalizedEmail);

        if (!passwordEncoder.matches(request.password(), customerAccount.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciais invalidas");
        }

        return toAuthResponse(customerAccount);
    }

    @Transactional(readOnly = true)
    public CustomerAccountMeResponseDTO me(CustomerAccountDetails currentCustomer) {
        if (currentCustomer == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Nao autenticado");
        }

        return new CustomerAccountMeResponseDTO(
                currentCustomer.getId(),
                currentCustomer.getDisplayName(),
                currentCustomer.getUsername(),
                currentCustomer.getDocument(),
                currentCustomer.getPhone(),
                currentCustomer.getPersonType()
        );
    }

    private CustomerAccountAuthResponseDTO toAuthResponse(CustomerAccount customerAccount) {
        return new CustomerAccountAuthResponseDTO(
                jwtService.generateCustomerToken(customerAccount),
                "Bearer",
                jwtService.getExpirationMs(),
                customerAccount.getId(),
                customerAccount.getName(),
                customerAccount.getEmail(),
                customerAccount.getPersonType()
        );
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeDocument(String document) {
        String normalized = trimToNull(document);
        return normalized == null ? null : normalized.replaceAll("[^0-9A-Za-z]", "");
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
