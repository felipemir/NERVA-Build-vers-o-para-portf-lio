package com.nervatech.nerva_build.controller;

import com.nervatech.nerva_build.dto.AdminOrderDetailResponseDTO;
import com.nervatech.nerva_build.dto.AdminOrderSummaryResponseDTO;
import com.nervatech.nerva_build.dto.OrderStatusUpdateRequestDTO;
import com.nervatech.nerva_build.dto.OrderStatusUpdateResponseDTO;
import com.nervatech.nerva_build.security.AppUserDetails;
import com.nervatech.nerva_build.service.AccessControlService;
import com.nervatech.nerva_build.service.OrderAdminService;
import com.nervatech.nerva_build.service.OrderStatusService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderAdminController {

    private final OrderAdminService orderAdminService;
    private final OrderStatusService orderStatusService;
    private final AccessControlService accessControlService;

    public OrderAdminController(
            OrderAdminService orderAdminService,
            OrderStatusService orderStatusService,
            AccessControlService accessControlService
    ) {
        this.orderAdminService = orderAdminService;
        this.orderStatusService = orderStatusService;
        this.accessControlService = accessControlService;
    }

    @GetMapping("/admin")
    public ResponseEntity<List<AdminOrderSummaryResponseDTO>> listForAdmin(
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "buyer", required = false) String buyer,
            @RequestParam(value = "createdFrom", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdFrom,
            @RequestParam(value = "createdTo", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdTo,
            @AuthenticationPrincipal AppUserDetails currentUser
    ) {
        Long storeId = accessControlService.requireOrderStore(currentUser);
        return ResponseEntity.ok(orderAdminService.listOrders(storeId, status, buyer, createdFrom, createdTo));
    }

    @GetMapping("/admin/{id}")
    public ResponseEntity<AdminOrderDetailResponseDTO> getForAdmin(
            @PathVariable Long id,
            @AuthenticationPrincipal AppUserDetails currentUser
    ) {
        Long storeId = accessControlService.requireOrderStore(currentUser);
        return ResponseEntity.ok(orderAdminService.getOrderDetail(storeId, id));
    }

    @PatchMapping("/admin/{id}/status")
    public ResponseEntity<OrderStatusUpdateResponseDTO> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody OrderStatusUpdateRequestDTO request,
            @AuthenticationPrincipal AppUserDetails currentUser
    ) {
        Long storeId = accessControlService.requireOrderStore(currentUser);
        return ResponseEntity.ok(orderStatusService.updateStatus(storeId, id, request.status(), currentUser));
    }
}
