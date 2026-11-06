package com.nervatech.nerva_build.service;

import com.nervatech.nerva_build.domain.model.Store;
import com.nervatech.nerva_build.repository.StoreRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;

@Service
public class PublicStoreContextService {

    private static final String STORE_HEADER = "X-Public-Store";

    private final StoreRepository storeRepository;

    public PublicStoreContextService(StoreRepository storeRepository) {
        this.storeRepository = storeRepository;
    }

    public Store resolveStore(HttpServletRequest request) {
        String subdomain = firstNonBlank(
                request == null ? null : request.getHeader(STORE_HEADER),
                request == null ? null : extractSubdomainFromHost(request.getServerName())
        );

        if (subdomain == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Contexto da loja publica e obrigatorio");
        }

        return storeRepository.findBySubdomainAndActiveTrue(normalizeSubdomain(subdomain))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Loja publica nao encontrada"));
    }

    private String extractSubdomainFromHost(String host) {
        String normalizedHost = normalizeSubdomain(host);
        if (normalizedHost == null || "localhost".equals(normalizedHost) || normalizedHost.matches("\\d+(\\.\\d+){3}")) {
            return null;
        }

        String[] parts = normalizedHost.split("\\.");
        return parts.length >= 3 ? parts[0] : null;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            String normalized = normalizeSubdomain(value);
            if (normalized != null) {
                return normalized;
            }
        }
        return null;
    }

    private String normalizeSubdomain(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim().toLowerCase(Locale.ROOT);
        return trimmed.isEmpty() ? null : trimmed;
    }
}
