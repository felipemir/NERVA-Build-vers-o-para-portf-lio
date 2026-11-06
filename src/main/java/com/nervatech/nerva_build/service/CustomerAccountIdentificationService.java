package com.nervatech.nerva_build.service;

import com.nervatech.nerva_build.domain.model.CustomerAccount;
import com.nervatech.nerva_build.dto.ApiFieldErrorDTO;
import com.nervatech.nerva_build.dto.PublicOrderBuyerRequestDTO;
import com.nervatech.nerva_build.exception.ApiValidationException;
import com.nervatech.nerva_build.repository.CustomerAccountRepository;
import com.nervatech.nerva_build.security.CustomerAccountDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class CustomerAccountIdentificationService {

    private final CustomerAccountRepository customerAccountRepository;
    private final PasswordEncoder passwordEncoder;

    public CustomerAccountIdentificationService(
            CustomerAccountRepository customerAccountRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.customerAccountRepository = customerAccountRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public CustomerAccount identifyOrCreate(PublicOrderBuyerRequestDTO buyer, CustomerAccountDetails currentCustomer) {
        String email = normalizeEmail(firstNonBlank(buyer.email(), currentCustomer == null ? null : currentCustomer.getUsername()));
        String phone = firstNonBlank(buyer.phone(), currentCustomer == null ? null : currentCustomer.getPhone());
        String document = normalizeDocument(firstNonBlank(buyer.document(), currentCustomer == null ? null : currentCustomer.getDocument()));

        CustomerAccount customerAccount = findExistingCustomerAccount(email, document, phone, currentCustomer);
        if (customerAccount == null) {
            customerAccount = new CustomerAccount();
            customerAccount.setEmail(email);
            customerAccount.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
            customerAccount.setActive(true);
        }

        applyBuyerData(customerAccount, buyer, email, phone, document);
        return customerAccountRepository.save(customerAccount);
    }

    private CustomerAccount findExistingCustomerAccount(
            String email,
            String document,
            String phone,
            CustomerAccountDetails currentCustomer
    ) {
        if (currentCustomer != null) {
            return customerAccountRepository.findByIdAndActiveTrue(currentCustomer.getId())
                    .orElseThrow(() -> new ApiValidationException("Erro de validacao", List.of(new ApiFieldErrorDTO(
                            "buyer",
                            "Comprador autenticado invalido"
                    ))));
        }

        CustomerAccount byEmail = email == null
                ? null
                : customerAccountRepository.findByEmailIgnoreCaseAndActiveTrue(email).orElse(null);
        if (byEmail != null) {
            throw new ApiValidationException("Erro de validacao", List.of(new ApiFieldErrorDTO(
                    "buyer.email",
                    "Ja existe conta para este email. Faca login para continuar com este comprador"
            )));
        }

        CustomerAccount byDocument = document == null
                ? null
                : customerAccountRepository.findByDocumentAndActiveTrue(document).orElse(null);
        if (byDocument != null) {
            throw new ApiValidationException("Erro de validacao", List.of(new ApiFieldErrorDTO(
                    "buyer.document",
                    "Ja existe conta para este documento. Faca login para continuar com este comprador"
            )));
        }

        return null;
    }

    private void applyBuyerData(
            CustomerAccount customerAccount,
            PublicOrderBuyerRequestDTO buyer,
            String email,
            String phone,
            String document
    ) {
        customerAccount.setName(buyer.name().trim());
        customerAccount.setEmail(email);
        customerAccount.setPhone(phone);
        customerAccount.setPersonType(normalizePersonType(buyer.personType()));

        if (document != null && !document.equals(customerAccount.getDocument())) {
            boolean documentExists = customerAccount.getId() == null
                    ? customerAccountRepository.existsByDocument(document)
                    : customerAccountRepository.existsByDocumentAndIdNot(document, customerAccount.getId());
            if (documentExists) {
                throw new ApiValidationException("Erro de validacao", List.of(new ApiFieldErrorDTO(
                        "buyer.document",
                        "Ja existe comprador com este documento"
                )));
            }
            customerAccount.setDocument(document);
        }
    }

    private String normalizePersonType(String personType) {
        String normalized = personType == null ? "PF" : personType.trim().toUpperCase(Locale.ROOT);
        if (!"PF".equals(normalized) && !"PJ".equals(normalized)) {
            throw new ApiValidationException("Erro de validacao", List.of(new ApiFieldErrorDTO(
                    "buyer.personType",
                    "personType deve ser PF ou PJ"
            )));
        }
        return normalized;
    }

    private String normalizeEmail(String email) {
        String normalized = trimToNull(email);
        return normalized == null ? null : normalized.toLowerCase(Locale.ROOT);
    }

    private String normalizeDocument(String document) {
        String normalized = trimToNull(document);
        return normalized == null ? null : normalized.replaceAll("[^0-9A-Za-z]", "");
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            String normalized = trimToNull(value);
            if (normalized != null) {
                return normalized;
            }
        }
        return null;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
