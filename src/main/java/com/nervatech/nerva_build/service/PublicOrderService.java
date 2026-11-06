package com.nervatech.nerva_build.service;

import com.nervatech.nerva_build.domain.model.Customer;
import com.nervatech.nerva_build.domain.model.CustomerAccount;
import com.nervatech.nerva_build.domain.model.Order;
import com.nervatech.nerva_build.domain.model.OrderItem;
import com.nervatech.nerva_build.domain.model.Product;
import com.nervatech.nerva_build.domain.model.Store;
import com.nervatech.nerva_build.dto.ApiFieldErrorDTO;
import com.nervatech.nerva_build.dto.OrderItemResponseDTO;
import com.nervatech.nerva_build.dto.OrderResponseDTO;
import com.nervatech.nerva_build.dto.PublicOrderBuyerRequestDTO;
import com.nervatech.nerva_build.dto.PublicOrderCreateRequestDTO;
import com.nervatech.nerva_build.dto.PublicOrderItemRequestDTO;
import com.nervatech.nerva_build.exception.ApiValidationException;
import com.nervatech.nerva_build.repository.CustomerRepository;
import com.nervatech.nerva_build.repository.OrderRepository;
import com.nervatech.nerva_build.repository.ProductRepository;
import com.nervatech.nerva_build.security.CustomerAccountDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class PublicOrderService {

    private static final String INITIAL_ORDER_STATUS = "PENDING";
    private static final String INITIAL_PAYMENT_STATUS = "PENDING";
    private static final String DEFAULT_UNIT_MEASURE = "UN";

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final StockService stockService;
    private final CustomerAccountIdentificationService customerAccountIdentificationService;

    public PublicOrderService(
            OrderRepository orderRepository,
            ProductRepository productRepository,
            CustomerRepository customerRepository,
            StockService stockService,
            CustomerAccountIdentificationService customerAccountIdentificationService
    ) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.customerRepository = customerRepository;
        this.stockService = stockService;
        this.customerAccountIdentificationService = customerAccountIdentificationService;
    }

    @Transactional
    public OrderResponseDTO createPublicOrder(
            Store store,
            PublicOrderCreateRequestDTO request,
            CustomerAccountDetails currentCustomer
    ) {
        validateBuyerIdentity(request.buyer(), currentCustomer);
        validateDuplicateProducts(request.items());

        Map<Long, Product> productsById = loadProducts(store.getId(), request.items());
        CustomerAccount customerAccount = customerAccountIdentificationService.identifyOrCreate(request.buyer(), currentCustomer);
        Customer customer = resolveStoreCustomer(store.getId(), request.buyer(), customerAccount);

        Order order = new Order();
        order.setStoreId(store.getId());
        order.setCustomerId(customer.getId());
        order.setCustomerAccountId(customerAccount.getId());
        order.setStatus(INITIAL_ORDER_STATUS);
        order.setPaymentStatus(INITIAL_PAYMENT_STATUS);
        order.setNotes(trimToNull(request.notes()));
        applyBuyerSnapshots(order, request.buyer(), customerAccount);

        BigDecimal total = BigDecimal.ZERO;
        for (int index = 0; index < request.items().size(); index++) {
            PublicOrderItemRequestDTO itemRequest = request.items().get(index);
            Product product = productsById.get(itemRequest.productId());
            stockService.validateAvailableForOrder(
                    store.getId(),
                    product,
                    itemRequest.quantity(),
                    "items[" + index + "].quantity"
            );

            BigDecimal quantity = BigDecimal.valueOf(itemRequest.quantity());
            BigDecimal unitPrice = product.getPrice();
            BigDecimal subtotal = unitPrice.multiply(quantity);

            OrderItem item = new OrderItem();
            item.setStoreId(store.getId());
            item.setProductId(product.getId());
            item.setQuantity(itemRequest.quantity());
            item.setUnitPrice(unitPrice);
            item.setSubtotal(subtotal);
            item.setSkuSnapshot(product.getSku());
            item.setNameSnapshot(product.getName());
            item.setUnitMeasure(DEFAULT_UNIT_MEASURE);

            order.addItem(item);
            total = total.add(subtotal);
        }

        order.setTotalAmount(total);
        Order saved = orderRepository.saveAndFlush(order);
        saved.setOrderNumber(buildOrderNumber(saved));

        return toResponse(orderRepository.save(saved));
    }

    private Map<Long, Product> loadProducts(Long storeId, List<PublicOrderItemRequestDTO> items) {
        List<Long> productIds = items.stream()
                .map(PublicOrderItemRequestDTO::productId)
                .toList();

        Map<Long, Product> productsById = productRepository.findByStore_IdAndIdInAndActiveTrue(storeId, productIds)
                .stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        List<ApiFieldErrorDTO> fieldErrors = new ArrayList<>();
        for (int index = 0; index < items.size(); index++) {
            Long productId = items.get(index).productId();
            if (!productsById.containsKey(productId)) {
                fieldErrors.add(new ApiFieldErrorDTO(
                        "items[" + index + "].productId",
                        "Produto nao encontrado, inativo ou indisponivel para esta loja"
                ));
            }
        }

        if (!fieldErrors.isEmpty()) {
            throw new ApiValidationException("Erro de validacao", fieldErrors);
        }

        return productsById;
    }

    private Customer resolveStoreCustomer(
            Long storeId,
            PublicOrderBuyerRequestDTO buyer,
            CustomerAccount customerAccount
    ) {
        String document = customerAccount.getDocument();
        String email = normalizeEmail(customerAccount.getEmail());

        Customer customer = findExistingCustomer(storeId, document, email);
        if (customer == null) {
            customer = new Customer();
            customer.setStoreId(storeId);
            customer.setActive(true);
        }

        boolean canOverwriteCustomerData = customer.getId() == null;
        applyBuyer(customer, buyer, customerAccount, document, email, canOverwriteCustomerData);
        return customerRepository.save(customer);
    }

    private Customer findExistingCustomer(Long storeId, String document, String email) {
        if (document != null) {
            return customerRepository.findByStoreIdAndDocument(storeId, document).orElse(null);
        }

        if (email != null) {
            return customerRepository.findFirstByStoreIdAndEmailIgnoreCaseOrderByIdAsc(storeId, email).orElse(null);
        }

        return null;
    }

    private void applyBuyer(
            Customer customer,
            PublicOrderBuyerRequestDTO buyer,
            CustomerAccount customerAccount,
            String document,
            String email,
            boolean canOverwriteCustomerData
    ) {
        customer.setName(resolveCustomerValue(
                customer.getName(),
                customerAccount.getName(),
                canOverwriteCustomerData
        ));
        customer.setDocument(resolveCustomerValue(customer.getDocument(), document, canOverwriteCustomerData));
        customer.setEmail(resolveCustomerValue(customer.getEmail(), email, canOverwriteCustomerData));
        customer.setPhone(resolveCustomerValue(
                customer.getPhone(),
                customerAccount.getPhone(),
                canOverwriteCustomerData
        ));
        customer.setPersonType(normalizePersonType(resolveCustomerValue(
                customer.getPersonType(),
                customerAccount.getPersonType(),
                canOverwriteCustomerData
        )));
        customer.setZipCode(resolveCustomerValue(customer.getZipCode(), trimToNull(buyer.zipCode()), canOverwriteCustomerData));
        customer.setStreet(resolveCustomerValue(customer.getStreet(), trimToNull(buyer.street()), canOverwriteCustomerData));
        customer.setNumber(resolveCustomerValue(customer.getNumber(), trimToNull(buyer.number()), canOverwriteCustomerData));
        customer.setNeighborhood(resolveCustomerValue(customer.getNeighborhood(), trimToNull(buyer.neighborhood()), canOverwriteCustomerData));
        customer.setCity(resolveCustomerValue(customer.getCity(), trimToNull(buyer.city()), canOverwriteCustomerData));
        customer.setState(resolveCustomerValue(customer.getState(), normalizeState(buyer.state()), canOverwriteCustomerData));
    }

    private void validateBuyerIdentity(PublicOrderBuyerRequestDTO buyer, CustomerAccountDetails currentCustomer) {
        String email = firstNonBlank(normalizeEmail(buyer.email()), currentCustomer == null ? null : currentCustomer.getUsername());
        String phone = firstNonBlank(trimToNull(buyer.phone()), currentCustomer == null ? null : currentCustomer.getPhone());

        List<ApiFieldErrorDTO> fieldErrors = new ArrayList<>();
        if (email == null) {
            fieldErrors.add(new ApiFieldErrorDTO("buyer.email", "Email do comprador e obrigatorio"));
        }
        if (phone == null) {
            fieldErrors.add(new ApiFieldErrorDTO("buyer.phone", "Celular do comprador e obrigatorio"));
        }
        if (trimToNull(buyer.zipCode()) == null) {
            fieldErrors.add(new ApiFieldErrorDTO("buyer.zipCode", "CEP do endereco e obrigatorio"));
        }
        if (trimToNull(buyer.street()) == null) {
            fieldErrors.add(new ApiFieldErrorDTO("buyer.street", "Rua do endereco e obrigatoria"));
        }
        if (trimToNull(buyer.number()) == null) {
            fieldErrors.add(new ApiFieldErrorDTO("buyer.number", "Numero do endereco e obrigatorio"));
        }
        if (trimToNull(buyer.neighborhood()) == null) {
            fieldErrors.add(new ApiFieldErrorDTO("buyer.neighborhood", "Bairro do endereco e obrigatorio"));
        }
        if (trimToNull(buyer.city()) == null) {
            fieldErrors.add(new ApiFieldErrorDTO("buyer.city", "Cidade do endereco e obrigatoria"));
        }
        if (trimToNull(buyer.state()) == null) {
            fieldErrors.add(new ApiFieldErrorDTO("buyer.state", "Estado do endereco e obrigatorio"));
        }

        if (!fieldErrors.isEmpty()) {
            throw new ApiValidationException("Erro de validacao", fieldErrors);
        }
    }

    private void validateDuplicateProducts(List<PublicOrderItemRequestDTO> items) {
        Set<Long> seen = new HashSet<>();
        List<ApiFieldErrorDTO> fieldErrors = new ArrayList<>();

        for (int index = 0; index < items.size(); index++) {
            Long productId = items.get(index).productId();
            if (productId != null && !seen.add(productId)) {
                fieldErrors.add(new ApiFieldErrorDTO(
                        "items[" + index + "].productId",
                        "Produto duplicado no pedido"
                ));
            }
        }

        if (!fieldErrors.isEmpty()) {
            throw new ApiValidationException("Erro de validacao", fieldErrors);
        }
    }

    private void applyBuyerSnapshots(Order order, PublicOrderBuyerRequestDTO buyer, CustomerAccount customerAccount) {
        order.setBuyerNameSnapshot(trimToNull(customerAccount.getName()));
        order.setBuyerEmailSnapshot(normalizeEmail(customerAccount.getEmail()));
        order.setBuyerPhoneSnapshot(trimToNull(customerAccount.getPhone()));
        order.setBuyerDocumentSnapshot(trimToNull(customerAccount.getDocument()));
        order.setBuyerPersonTypeSnapshot(normalizePersonType(customerAccount.getPersonType()));
        order.setDeliveryZipCodeSnapshot(trimToNull(buyer.zipCode()));
        order.setDeliveryStreetSnapshot(trimToNull(buyer.street()));
        order.setDeliveryNumberSnapshot(trimToNull(buyer.number()));
        order.setDeliveryNeighborhoodSnapshot(trimToNull(buyer.neighborhood()));
        order.setDeliveryCitySnapshot(trimToNull(buyer.city()));
        order.setDeliveryStateSnapshot(normalizeState(buyer.state()));
    }

    private String buildOrderNumber(Order order) {
        return "PED-" + order.getStoreId() + "-" + String.format("%06d", order.getId());
    }

    private OrderResponseDTO toResponse(Order order) {
        return new OrderResponseDTO(
                order.getId(),
                order.getOrderNumber(),
                order.getStoreId(),
                order.getCustomerId(),
                order.getCustomerAccountId(),
                order.getStatus(),
                order.getPaymentStatus(),
                order.getTotalAmount(),
                order.getItems().stream().map(this::toItemResponse).toList(),
                order.getCreatedAt()
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

    private String normalizePersonType(String personType) {
        String normalized = personType == null ? "PF" : personType.trim().toUpperCase(Locale.ROOT);
        if (!"PF".equals(normalized) && !"PJ".equals(normalized)) {
            throw new ApiValidationException("Erro de validacao", List.of(new ApiFieldErrorDTO(
                    "buyer.personType",
                    "personType deve ser PF ou PJ"
            )));
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

    private String firstNonBlank(String... values) {
        for (String value : values) {
            String normalized = trimToNull(value);
            if (normalized != null) {
                return normalized;
            }
        }
        return null;
    }

    private String resolveCustomerValue(String currentValue, String incomingValue, boolean canOverwrite) {
        if (canOverwrite) {
            return incomingValue;
        }

        return firstNonBlank(currentValue, incomingValue);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
