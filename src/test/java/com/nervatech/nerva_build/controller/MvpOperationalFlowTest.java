package com.nervatech.nerva_build.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nervatech.nerva_build.domain.model.AppUser;
import com.nervatech.nerva_build.domain.model.CustomerAccount;
import com.nervatech.nerva_build.domain.model.Order;
import com.nervatech.nerva_build.domain.model.OrderItem;
import com.nervatech.nerva_build.domain.model.Product;
import com.nervatech.nerva_build.domain.model.Store;
import com.nervatech.nerva_build.repository.AppUserRepository;
import com.nervatech.nerva_build.repository.CustomerAccountRepository;
import com.nervatech.nerva_build.repository.OrderRepository;
import com.nervatech.nerva_build.repository.ProductRepository;
import com.nervatech.nerva_build.repository.StoreRepository;
import com.nervatech.nerva_build.security.AppUserDetails;
import com.nervatech.nerva_build.security.CustomerAccountDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("MVP Operational Flow Tests")
class MvpOperationalFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private CustomerAccountRepository customerAccountRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private Store store;
    private Product product;
    private AppUserDetails admin;
    private AppUserDetails operator;
    private CustomerAccountDetails buyer;
    private CustomerAccountDetails otherBuyer;

    @BeforeEach
    void setUp() {
        store = createStore("MVP Store", "mvp-store");
        product = createProduct(store, "Cimento Teste", "CIM-MVP", BigDecimal.valueOf(10));
        admin = createUser("admin@mvp.test", "STORE_ADMIN");
        operator = createUser("operator@mvp.test", "STORE_OPERATOR");
        buyer = createCustomerAccount("buyer@mvp.test");
        otherBuyer = createCustomerAccount("other@mvp.test");
    }

    @Test
    @DisplayName("Admin should confirm order, decrement stock, then cancel and restore stock")
    void updateStatus_shouldCommitAndRestoreStock() throws Exception {
        Order order = createOrder(product, buyer.getId(), 3);

        mockMvc.perform(patch("/api/orders/admin/{id}/status", order.getId())
                        .with(user(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "CONFIRMED"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("CONFIRMED")))
                .andExpect(jsonPath("$.stockCommitted", is(true)))
                .andExpect(jsonPath("$.updatedByUserId", is(admin.getId().intValue())));

        Product afterConfirmation = productRepository.findById(product.getId()).orElseThrow();
        assertThat(afterConfirmation.getStockQuantity()).isEqualByComparingTo(BigDecimal.valueOf(7));

        mockMvc.perform(patch("/api/orders/admin/{id}/status", order.getId())
                        .with(user(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "CANCELED"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("CANCELED")))
                .andExpect(jsonPath("$.stockCommitted", is(false)));

        Product afterCancel = productRepository.findById(product.getId()).orElseThrow();
        assertThat(afterCancel.getStockQuantity()).isEqualByComparingTo(BigDecimal.valueOf(10));
    }

    @Test
    @DisplayName("Admin should not confirm order when stock is insufficient")
    void updateStatus_shouldRejectInsufficientStock() throws Exception {
        Order order = createOrder(product, buyer.getId(), 11);

        mockMvc.perform(patch("/api/orders/admin/{id}/status", order.getId())
                        .with(user(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "CONFIRMED"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors", hasSize(1)));

        Product afterAttempt = productRepository.findById(product.getId()).orElseThrow();
        assertThat(afterAttempt.getStockQuantity()).isEqualByComparingTo(BigDecimal.valueOf(10));
    }

    @Test
    @DisplayName("Operator should not update order status")
    void updateStatus_shouldRejectOperator() throws Exception {
        Order order = createOrder(product, buyer.getId(), 1);

        mockMvc.perform(patch("/api/orders/admin/{id}/status", order.getId())
                        .with(user(operator))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "CONFIRMED"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Buyer should list and view only own orders")
    void customerOrders_shouldReturnOnlyCurrentBuyerOrders() throws Exception {
        Order ownOrder = createOrder(product, buyer.getId(), 1);
        Order otherOrder = createOrder(product, otherBuyer.getId(), 1);

        mockMvc.perform(get("/api/customer-orders")
                        .with(user(buyer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(ownOrder.getId().intValue())));

        mockMvc.perform(get("/api/customer-orders/{id}", ownOrder.getId())
                        .with(user(buyer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(ownOrder.getId().intValue())))
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.address.city", is("Sao Paulo")));

        mockMvc.perform(get("/api/customer-orders/{id}", otherOrder.getId())
                        .with(user(buyer)))
                .andExpect(status().isNotFound());
    }

    private Store createStore(String name, String subdomain) {
        Store createdStore = new Store();
        createdStore.setName(name);
        createdStore.setSubdomain(subdomain);
        return storeRepository.save(createdStore);
    }

    private Product createProduct(Store productStore, String name, String sku, BigDecimal stock) {
        Product createdProduct = new Product();
        createdProduct.setStore(productStore);
        createdProduct.setName(name);
        createdProduct.setSku(sku);
        createdProduct.setUnit("SC");
        createdProduct.setPrice(BigDecimal.valueOf(39.90));
        createdProduct.setStockQuantity(stock);
        createdProduct.setActive(true);
        return productRepository.save(createdProduct);
    }

    private AppUserDetails createUser(String email, String role) {
        AppUser user = new AppUser();
        user.setStoreId(store.getId());
        user.setName(role + " User");
        user.setEmail(email);
        user.setPassword("password");
        user.setRole(role);
        user.setActive(true);
        return new AppUserDetails(appUserRepository.save(user));
    }

    private CustomerAccountDetails createCustomerAccount(String email) {
        CustomerAccount customerAccount = new CustomerAccount();
        customerAccount.setName("Buyer");
        customerAccount.setEmail(email);
        customerAccount.setPassword("password");
        customerAccount.setDocument(email.replaceAll("[^0-9A-Za-z]", ""));
        customerAccount.setPhone("11999999999");
        customerAccount.setPersonType("PF");
        customerAccount.setActive(true);
        return new CustomerAccountDetails(customerAccountRepository.save(customerAccount));
    }

    private Order createOrder(Product orderProduct, Long customerAccountId, int quantity) {
        Order order = new Order();
        order.setStoreId(store.getId());
        order.setCustomerAccountId(customerAccountId);
        order.setStatus("PENDING");
        order.setPaymentStatus("PENDING");
        order.setTotalAmount(orderProduct.getPrice().multiply(BigDecimal.valueOf(quantity)));
        order.setBuyerNameSnapshot("Buyer");
        order.setBuyerEmailSnapshot("buyer@mvp.test");
        order.setBuyerPhoneSnapshot("11999999999");
        order.setBuyerPersonTypeSnapshot("PF");
        order.setDeliveryZipCodeSnapshot("01001000");
        order.setDeliveryStreetSnapshot("Rua Teste");
        order.setDeliveryNumberSnapshot("123");
        order.setDeliveryNeighborhoodSnapshot("Centro");
        order.setDeliveryCitySnapshot("Sao Paulo");
        order.setDeliveryStateSnapshot("SP");

        OrderItem item = new OrderItem();
        item.setStoreId(store.getId());
        item.setProductId(orderProduct.getId());
        item.setQuantity(quantity);
        item.setUnitPrice(orderProduct.getPrice());
        item.setSubtotal(orderProduct.getPrice().multiply(BigDecimal.valueOf(quantity)));
        item.setSkuSnapshot(orderProduct.getSku());
        item.setNameSnapshot(orderProduct.getName());
        item.setUnitMeasure(orderProduct.getUnit());
        order.addItem(item);

        Order saved = orderRepository.saveAndFlush(order);
        saved.setOrderNumber("PED-" + saved.getStoreId() + "-" + String.format("%06d", saved.getId()));
        return orderRepository.saveAndFlush(saved);
    }
}
