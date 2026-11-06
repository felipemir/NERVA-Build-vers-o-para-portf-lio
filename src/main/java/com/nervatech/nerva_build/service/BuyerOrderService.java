package com.nervatech.nerva_build.service;

import com.nervatech.nerva_build.domain.model.Order;
import com.nervatech.nerva_build.domain.model.OrderItem;
import com.nervatech.nerva_build.dto.AdminOrderAddressResponseDTO;
import com.nervatech.nerva_build.dto.BuyerOrderDetailResponseDTO;
import com.nervatech.nerva_build.dto.BuyerOrderSummaryResponseDTO;
import com.nervatech.nerva_build.dto.OrderItemResponseDTO;
import com.nervatech.nerva_build.repository.OrderRepository;
import com.nervatech.nerva_build.security.CustomerAccountDetails;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class BuyerOrderService {

    private final OrderRepository orderRepository;

    public BuyerOrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Transactional(readOnly = true)
    public List<BuyerOrderSummaryResponseDTO> listMyOrders(CustomerAccountDetails currentCustomer) {
        Long customerAccountId = requireCustomerId(currentCustomer);
        return orderRepository.findByCustomerAccountIdOrderByCreatedAtDescIdDesc(customerAccountId)
                .stream()
                .map(this::toSummary)
                .toList();
    }

    @Transactional(readOnly = true)
    public BuyerOrderDetailResponseDTO getMyOrder(Long orderId, CustomerAccountDetails currentCustomer) {
        Long customerAccountId = requireCustomerId(currentCustomer);
        Order order = orderRepository.findByIdAndCustomerAccountId(orderId, customerAccountId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido nao encontrado"));

        return toDetail(order);
    }

    private Long requireCustomerId(CustomerAccountDetails currentCustomer) {
        if (currentCustomer == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Comprador nao autenticado");
        }
        return currentCustomer.getId();
    }

    private BuyerOrderSummaryResponseDTO toSummary(Order order) {
        return new BuyerOrderSummaryResponseDTO(
                order.getId(),
                order.getOrderNumber(),
                order.getStoreId(),
                order.getStatus(),
                order.getPaymentStatus(),
                order.getTotalAmount(),
                order.getItems().size(),
                order.getCreatedAt()
        );
    }

    private BuyerOrderDetailResponseDTO toDetail(Order order) {
        return new BuyerOrderDetailResponseDTO(
                order.getId(),
                order.getOrderNumber(),
                order.getStoreId(),
                order.getStatus(),
                order.getPaymentStatus(),
                order.getTotalAmount(),
                order.getItems().stream().map(this::toItemResponse).toList(),
                toAddress(order),
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }

    private OrderItemResponseDTO toItemResponse(OrderItem item) {
        return new OrderItemResponseDTO(
                item.getProductId(),
                item.getSkuSnapshot(),
                item.getNameSnapshot(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getSubtotal()
        );
    }

    private AdminOrderAddressResponseDTO toAddress(Order order) {
        return new AdminOrderAddressResponseDTO(
                order.getDeliveryZipCodeSnapshot(),
                order.getDeliveryStreetSnapshot(),
                order.getDeliveryNumberSnapshot(),
                order.getDeliveryNeighborhoodSnapshot(),
                order.getDeliveryCitySnapshot(),
                order.getDeliveryStateSnapshot()
        );
    }
}
