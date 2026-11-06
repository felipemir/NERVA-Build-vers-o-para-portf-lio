package com.nervatech.nerva_build.controller;

import com.nervatech.nerva_build.dto.BuyerOrderDetailResponseDTO;
import com.nervatech.nerva_build.dto.BuyerOrderSummaryResponseDTO;
import com.nervatech.nerva_build.security.CustomerAccountDetails;
import com.nervatech.nerva_build.service.BuyerOrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/customer-orders")
public class BuyerOrderController {

    private final BuyerOrderService buyerOrderService;

    public BuyerOrderController(BuyerOrderService buyerOrderService) {
        this.buyerOrderService = buyerOrderService;
    }

    @GetMapping
    public ResponseEntity<List<BuyerOrderSummaryResponseDTO>> listMyOrders(
            @AuthenticationPrincipal CustomerAccountDetails currentCustomer
    ) {
        return ResponseEntity.ok(buyerOrderService.listMyOrders(currentCustomer));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BuyerOrderDetailResponseDTO> getMyOrder(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomerAccountDetails currentCustomer
    ) {
        return ResponseEntity.ok(buyerOrderService.getMyOrder(id, currentCustomer));
    }
}
