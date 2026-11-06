package com.nervatech.nerva_build.service;

import com.nervatech.nerva_build.domain.model.Customer;
import com.nervatech.nerva_build.dto.CustomerRequestDTO;
import com.nervatech.nerva_build.dto.CustomerResponseDTO;
import com.nervatech.nerva_build.repository.CustomerRepository;
import com.nervatech.nerva_build.security.AppUserDetails;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class CustomerService {

    private static final Set<String> ALLOWED_ROLES = Set.of("STORE_OWNER", "STORE_ADMIN", "STORE_OPERATOR");

    private final CustomerRepository customerRepository;

    public CustomerService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Transactional
    public CustomerResponseDTO create(CustomerRequestDTO request, AppUserDetails currentUser) {
        Long storeId = requireAuthorizedStore(currentUser);
        String normalizedDocument = normalizeDocument(request.document());

        validateDocumentUniqueness(storeId, normalizedDocument, null);

        Customer customer = new Customer();
        customer.setStoreId(storeId);
        applyChanges(customer, request, normalizedDocument);

        return toResponse(customerRepository.save(customer));
    }

    @Transactional(readOnly = true)
    public List<CustomerResponseDTO> search(String term, AppUserDetails currentUser) {
        Long storeId = requireAuthorizedStore(currentUser);
        return customerRepository.searchByStoreId(storeId, normalizeTerm(term))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CustomerResponseDTO getById(Long customerId, AppUserDetails currentUser) {
        Long storeId = requireAuthorizedStore(currentUser);
        return toResponse(findEntity(storeId, customerId));
    }

    @Transactional
    public CustomerResponseDTO update(Long customerId, CustomerRequestDTO request, AppUserDetails currentUser) {
        Long storeId = requireAuthorizedStore(currentUser);
        Customer customer = findEntity(storeId, customerId);
        String normalizedDocument = normalizeDocument(request.document());

        validateDocumentUniqueness(storeId, normalizedDocument, customerId);

        applyChanges(customer, request, normalizedDocument);
        return toResponse(customerRepository.save(customer));
    }

    @Transactional
    public void deactivate(Long customerId, AppUserDetails currentUser) {
        Long storeId = requireAuthorizedStore(currentUser);
        Customer customer = findEntity(storeId, customerId);
        customer.setActive(false);
        customerRepository.save(customer);
    }

    private Long requireAuthorizedStore(AppUserDetails currentUser) {
        if (currentUser == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Nao autenticado");
        }

        Long storeId = currentUser.getStoreId();
        if (storeId == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Usuario sem loja associada");
        }

        String role = currentUser.getRole() == null ? "" : currentUser.getRole().trim().toUpperCase(Locale.ROOT);
        if (!ALLOWED_ROLES.contains(role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Perfil sem permissao para gerenciar clientes");
        }

        return storeId;
    }

    private Customer findEntity(Long storeId, Long customerId) {
        return customerRepository.findByIdAndStoreId(customerId, storeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente nao encontrado"));
    }

    private void validateDocumentUniqueness(Long storeId, String document, Long currentCustomerId) {
        if (document == null) {
            return;
        }

        boolean exists = currentCustomerId == null
                ? customerRepository.existsByStoreIdAndDocument(storeId, document)
                : customerRepository.existsByStoreIdAndDocumentAndIdNot(storeId, document, currentCustomerId);

        if (exists) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ja existe cliente com este documento na loja");
        }
    }

    private void applyChanges(Customer customer, CustomerRequestDTO request, String normalizedDocument) {
        customer.setName(request.name().trim());
        customer.setDocument(normalizedDocument);
        customer.setEmail(normalizeEmail(request.email()));
        customer.setPhone(trimToNull(request.phone()));
        customer.setZipCode(trimToNull(request.zipCode()));
        customer.setStreet(trimToNull(request.street()));
        customer.setNumber(trimToNull(request.number()));
        customer.setNeighborhood(trimToNull(request.neighborhood()));
        customer.setCity(trimToNull(request.city()));
        customer.setState(normalizeState(request.state()));
        customer.setPersonType(normalizePersonType(request.personType()));
        customer.setActive(request.active() == null ? Boolean.TRUE : request.active());
    }

    private CustomerResponseDTO toResponse(Customer customer) {
        return new CustomerResponseDTO(
                customer.getId(),
                customer.getStoreId(),
                customer.getName(),
                customer.getDocument(),
                customer.getEmail(),
                customer.getPhone(),
                customer.getZipCode(),
                customer.getStreet(),
                customer.getNumber(),
                customer.getNeighborhood(),
                customer.getCity(),
                customer.getState(),
                customer.getPersonType(),
                customer.getActive()
        );
    }

    private String normalizePersonType(String personType) {
        String normalized = personType == null ? "PF" : personType.trim().toUpperCase(Locale.ROOT);
        if (!"PF".equals(normalized) && !"PJ".equals(normalized)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "personType deve ser PF ou PJ");
        }
        return normalized;
    }

    private String normalizeState(String state) {
        String normalized = trimToNull(state);
        return normalized == null ? null : normalized.toUpperCase(Locale.ROOT);
    }

    private String normalizeEmail(String email) {
        String normalized = trimToNull(email);
        return normalized == null ? null : normalized.toLowerCase(Locale.ROOT);
    }

    private String normalizeDocument(String document) {
        String normalized = trimToNull(document);
        return normalized == null ? null : normalized.replaceAll("[^0-9A-Za-z]", "");
    }

    private String normalizeTerm(String term) {
        return trimToNull(term);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
