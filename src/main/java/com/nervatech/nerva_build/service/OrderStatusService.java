package com.nervatech.nerva_build.service;

import com.nervatech.nerva_build.domain.model.Order;
import com.nervatech.nerva_build.dto.OrderStatusUpdateResponseDTO;
import com.nervatech.nerva_build.repository.OrderRepository;
import com.nervatech.nerva_build.security.AppUserDetails;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class OrderStatusService {

    private static final String PENDING = "PENDING";
    private static final String CONFIRMED = "CONFIRMED";
    private static final String SEPARATING = "SEPARATING";
    private static final String SHIPPED = "SHIPPED";
    private static final String DELIVERED = "DELIVERED";
    private static final String CANCELED = "CANCELED";

    private static final Set<String> VALID_STATUSES = Set.of(
            PENDING,
            CONFIRMED,
            SEPARATING,
            SHIPPED,
            DELIVERED,
            CANCELED
    );

    private static final Map<String, List<String>> ALLOWED_TRANSITIONS = Map.of(
            PENDING, List.of(CONFIRMED, CANCELED),
            CONFIRMED, List.of(SEPARATING, CANCELED),
            SEPARATING, List.of(SHIPPED, CANCELED),
            SHIPPED, List.of(DELIVERED),
            DELIVERED, List.of(),
            CANCELED, List.of()
    );

    private final OrderRepository orderRepository;
    private final StockService stockService;

    public OrderStatusService(OrderRepository orderRepository, StockService stockService) {
        this.orderRepository = orderRepository;
        this.stockService = stockService;
    }

    @Transactional
    public OrderStatusUpdateResponseDTO updateStatus(Long storeId, Long orderId, String requestedStatus, AppUserDetails currentUser) {
        String targetStatus = normalizeStatus(requestedStatus);
        Order order = orderRepository.findWithLockByIdAndStoreId(orderId, storeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido nao encontrado"));

        String currentStatus = normalizeStatus(order.getStatus());
        validateTransition(currentStatus, targetStatus);

        if (PENDING.equals(currentStatus) && CONFIRMED.equals(targetStatus)) {
            stockService.commitStock(order);
            order.setStockCommitted(true);
        }

        if (CANCELED.equals(targetStatus) && Boolean.TRUE.equals(order.getStockCommitted())) {
            stockService.restoreStock(order);
            order.setStockCommitted(false);
        }

        order.setStatus(targetStatus);
        order.setUpdatedByUserId(currentUser.getId());

        return toResponse(orderRepository.save(order));
    }

    private void validateTransition(String currentStatus, String targetStatus) {
        List<String> allowedTargets = ALLOWED_TRANSITIONS.getOrDefault(currentStatus, List.of());
        if (!allowedTargets.contains(targetStatus)) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "Transicao de status invalida. Status atual: " + currentStatus
                            + ". Status permitidos: " + String.join(", ", allowedTargets)
            );
        }
    }

    private String normalizeStatus(String status) {
        if (status == null || status.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Status alvo e obrigatorio");
        }

        String normalized = status.trim().toUpperCase(Locale.ROOT);
        if (!VALID_STATUSES.contains(normalized)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Status de pedido invalido");
        }
        return normalized;
    }

    private OrderStatusUpdateResponseDTO toResponse(Order order) {
        return new OrderStatusUpdateResponseDTO(
                order.getId(),
                order.getOrderNumber(),
                order.getStatus(),
                order.getStockCommitted(),
                order.getUpdatedByUserId(),
                order.getUpdatedAt()
        );
    }
}
