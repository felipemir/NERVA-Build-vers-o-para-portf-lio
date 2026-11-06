package com.nervatech.nerva_build.service;

import com.nervatech.nerva_build.domain.model.CustomerAccount;
import com.nervatech.nerva_build.repository.CustomerAccountRepository;
import com.nervatech.nerva_build.security.CustomerAccountDetails;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CustomerAccountAuthService {

    private final CustomerAccountRepository customerAccountRepository;

    public CustomerAccountAuthService(CustomerAccountRepository customerAccountRepository) {
        this.customerAccountRepository = customerAccountRepository;
    }

    @Transactional(readOnly = true)
    public CustomerAccountDetails loadById(Long id) {
        CustomerAccount customerAccount = customerAccountRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Cliente invalido"));
        return new CustomerAccountDetails(customerAccount);
    }

    @Transactional(readOnly = true)
    public CustomerAccount findByEmailOrThrow(String email) {
        return customerAccountRepository.findByEmailIgnoreCaseAndActiveTrue(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciais invalidas"));
    }

    @Transactional
    public CustomerAccount save(CustomerAccount customerAccount) {
        return customerAccountRepository.save(customerAccount);
    }
}
