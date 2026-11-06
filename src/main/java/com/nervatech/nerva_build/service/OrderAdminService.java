package com.nervatech.nerva_build.service;

import com.nervatech.nerva_build.domain.model.Customer;
import com.nervatech.nerva_build.domain.model.CustomerAccount;
import com.nervatech.nerva_build.domain.model.Order;
import com.nervatech.nerva_build.domain.model.OrderItem;
import com.nervatech.nerva_build.dto.AdminOrderAddressResponseDTO;
import com.nervatech.nerva_build.dto.AdminOrderBuyerResponseDTO;
import com.nervatech.nerva_build.dto.AdminOrderDetailResponseDTO;
import com.nervatech.nerva_build.dto.AdminOrderItemSummaryResponseDTO;
import com.nervatech.nerva_build.dto.AdminOrderSummaryResponseDTO;
import com.nervatech.nerva_build.repository.OrderRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class OrderAdminService {

    private static final Set<String> ALLOWED_STATUSES = Set.of(
            "PENDING",
            "CONFIRMED",
            "SEPARATING",
            "SHIPPED",
            "DELIVERED",
            "CANCELED",
            "PAID"
    );

    private final OrderRepository orderRepository;

    public OrderAdminService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Transactional(readOnly = true)
    public List<AdminOrderSummaryResponseDTO> listOrders(
            Long storeId,
            String status,
            String buyer,
            LocalDate createdFrom,
            LocalDate createdTo
    ) {
        String normalizedStatus = normalizeStatus(status);
        if (normalizedStatus == null) {
            normalizedStatus = "";
        }
        String buyerTerm = trimToNull(buyer);
        if (buyerTerm == null) {
            buyerTerm = "";
        }
        LocalDateTime from = createdFrom == null ? LocalDateTime.of(1900, 1, 1, 0, 0) : createdFrom.atStartOfDay();
        LocalDateTime toExclusive = createdTo == null ? LocalDateTime.of(9999, 12, 31, 0, 0) : createdTo.plusDays(1).atStartOfDay();

        if (!from.isBefore(toExclusive)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Periodo de criacao invalido");
        }

        return orderRepository.searchAdminOrders(storeId, normalizedStatus, buyerTerm, from, toExclusive)
                .stream()
                .map(this::toSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public AdminOrderDetailResponseDTO getOrderDetail(Long storeId, Long orderId) {
        Order order = orderRepository.findByIdAndStoreId(orderId, storeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido nao encontrado"));

        return toDetail(order);
    }

    private String normalizeStatus(String status) {
        String normalized = trimToNull(status);
        if (normalized == null) {
            return null;
        }

        normalized = normalized.toUpperCase(Locale.ROOT);
        if (!ALLOWED_STATUSES.contains(normalized)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Status de pedido invalido");
        }
        return normalized;
    }

    private AdminOrderSummaryResponseDTO toSummary(Order order) {
        Customer customer = order.getCustomer();
        CustomerAccount customerAccount = order.getCustomerAccount();

        String buyerName = firstNonBlank(
                order.getBuyerNameSnapshot(),
                customerAccount == null ? null : customerAccount.getName(),
                customer == null ? null : customer.getName()
        );
        String buyerEmail = firstNonBlank(
                order.getBuyerEmailSnapshot(),
                customerAccount == null ? null : customerAccount.getEmail(),
                customer == null ? null : customer.getEmail()
        );
        String buyerPhone = firstNonBlank(
                order.getBuyerPhoneSnapshot(),
                customerAccount == null ? null : customerAccount.getPhone(),
                customer == null ? null : customer.getPhone()
        );

        return new AdminOrderSummaryResponseDTO(
                order.getId(),
                order.getOrderNumber(),
                order.getStatus(),
                order.getPaymentStatus(),
                order.getTotalAmount(),
                order.getCustomerId(),
                order.getCustomerAccountId(),
                buyerName,
                buyerEmail,
                buyerPhone,
                order.getItems().size(),
                order.getItems().stream().map(this::toItemSummary).toList(),
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }

    private AdminOrderItemSummaryResponseDTO toItemSummary(OrderItem item) {
        return new AdminOrderItemSummaryResponseDTO(
                item.getProductId(),
                item.getSkuSnapshot(),
                item.getNameSnapshot(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getSubtotal()
        );
    }

    private AdminOrderDetailResponseDTO toDetail(Order order) {
        return new AdminOrderDetailResponseDTO(
                order.getId(),
                order.getOrderNumber(),
                order.getStoreId(),
                order.getStatus(),
                order.getPaymentStatus(),
                order.getPaymentMethod(),
                order.getTotalAmount(),
                order.getNotes(),
                toBuyer(order),
                toAddress(order),
                order.getItems().stream().map(this::toItemSummary).toList(),
                order.getCreatedAt(),
                order.getUpdatedAt(),
                order.getUserId(),
                order.getUpdatedByUserId()
        );
    }

    private AdminOrderBuyerResponseDTO toBuyer(Order order) {
        Customer customer = order.getCustomer();
        CustomerAccount customerAccount = order.getCustomerAccount();

        return new AdminOrderBuyerResponseDTO(
                order.getCustomerId(),
                order.getCustomerAccountId(),
                firstNonBlank(
                        order.getBuyerNameSnapshot(),
                        customerAccount == null ? null : customerAccount.getName(),
                        customer == null ? null : customer.getName()
                ),
                firstNonBlank(
                        order.getBuyerEmailSnapshot(),
                        customerAccount == null ? null : customerAccount.getEmail(),
                        customer == null ? null : customer.getEmail()
                ),
                firstNonBlank(
                        order.getBuyerPhoneSnapshot(),
                        customerAccount == null ? null : customerAccount.getPhone(),
                        customer == null ? null : customer.getPhone()
                ),
                firstNonBlank(
                        order.getBuyerDocumentSnapshot(),
                        customerAccount == null ? null : customerAccount.getDocument(),
                        customer == null ? null : customer.getDocument()
                ),
                firstNonBlank(
                        order.getBuyerPersonTypeSnapshot(),
                        customerAccount == null ? null : customerAccount.getPersonType(),
                        customer == null ? null : customer.getPersonType()
                )
        );
    }

    private AdminOrderAddressResponseDTO toAddress(Order order) {
        Customer customer = order.getCustomer();
        if (customer == null) {
            if (order.getDeliveryZipCodeSnapshot() == null
                    && order.getDeliveryStreetSnapshot() == null
                    && order.getDeliveryNumberSnapshot() == null
                    && order.getDeliveryNeighborhoodSnapshot() == null
                    && order.getDeliveryCitySnapshot() == null
                    && order.getDeliveryStateSnapshot() == null) {
                return null;
            }
        }

        return new AdminOrderAddressResponseDTO(
                firstNonBlank(order.getDeliveryZipCodeSnapshot(), customer == null ? null : customer.getZipCode()),
                firstNonBlank(order.getDeliveryStreetSnapshot(), customer == null ? null : customer.getStreet()),
                firstNonBlank(order.getDeliveryNumberSnapshot(), customer == null ? null : customer.getNumber()),
                firstNonBlank(order.getDeliveryNeighborhoodSnapshot(), customer == null ? null : customer.getNeighborhood()),
                firstNonBlank(order.getDeliveryCitySnapshot(), customer == null ? null : customer.getCity()),
                firstNonBlank(order.getDeliveryStateSnapshot(), customer == null ? null : customer.getState())
        );
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
