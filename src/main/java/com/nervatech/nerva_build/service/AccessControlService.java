package com.nervatech.nerva_build.service;

import com.nervatech.nerva_build.security.AppUserDetails;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;
import java.util.Set;

@Service
public class AccessControlService {

    private static final Set<String> CATALOG_MANAGER_ROLES = Set.of("STORE_OWNER", "STORE_ADMIN", "STORE_OPERATOR");
    private static final Set<String> ORDER_MANAGER_ROLES = Set.of("STORE_OWNER", "STORE_ADMIN");
    private static final Set<String> STORE_MANAGER_ROLES = Set.of("STORE_OWNER", "STORE_ADMIN");

    public Long requireCatalogStore(AppUserDetails currentUser) {
        return requireStoreAccess(currentUser, CATALOG_MANAGER_ROLES, "Perfil sem permissao para gerenciar catalogo");
    }

    public Long requireOrderStore(AppUserDetails currentUser) {
        return requireStoreAccess(currentUser, ORDER_MANAGER_ROLES, "Perfil sem permissao para consultar pedidos");
    }

    public Long requireStoreManagement(AppUserDetails currentUser) {
        return requireStoreAccess(currentUser, STORE_MANAGER_ROLES, "Perfil sem permissao para gerenciar lojas");
    }

    private Long requireStoreAccess(AppUserDetails currentUser, Set<String> allowedRoles, String forbiddenMessage) {
        if (currentUser == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Nao autenticado");
        }

        String normalizedRole = currentUser.getRole() == null
                ? ""
                : currentUser.getRole().trim().toUpperCase(Locale.ROOT);

        if (!allowedRoles.contains(normalizedRole)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, forbiddenMessage);
        }

        return currentUser.getStoreId();
    }
}
