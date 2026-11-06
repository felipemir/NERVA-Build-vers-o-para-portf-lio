package com.nervatech.nerva_build.controller;

import com.nervatech.nerva_build.domain.model.Store;
import com.nervatech.nerva_build.dto.OrderResponseDTO;
import com.nervatech.nerva_build.dto.PublicOrderCreateRequestDTO;
import com.nervatech.nerva_build.security.CustomerAccountDetails;
import com.nervatech.nerva_build.service.PublicOrderService;
import com.nervatech.nerva_build.service.PublicStoreContextService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/public/orders")
public class PublicOrderController {

    private final PublicOrderService publicOrderService;
    private final PublicStoreContextService publicStoreContextService;

    public PublicOrderController(
            PublicOrderService publicOrderService,
            PublicStoreContextService publicStoreContextService
    ) {
        this.publicOrderService = publicOrderService;
        this.publicStoreContextService = publicStoreContextService;
    }

    @PostMapping
    public ResponseEntity<OrderResponseDTO> create(
            @Valid @RequestBody PublicOrderCreateRequestDTO request,
            @AuthenticationPrincipal CustomerAccountDetails currentCustomer,
            HttpServletRequest httpRequest
    ) {
        if (currentCustomer == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Comprador precisa estar autenticado para criar pedido");
        }

        Store store = publicStoreContextService.resolveStore(httpRequest);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(publicOrderService.createPublicOrder(store, request, currentCustomer));
    }
}
