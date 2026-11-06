package com.nervatech.nerva_build.domain.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @Column(name = "customer_id")
    private Long customerId;

    @Column(name = "customer_account_id")
    private Long customerAccountId;

    @ManyToOne
    @JoinColumn(name = "customer_id", insertable = false, updatable = false)
    private Customer customer;

    @ManyToOne
    @JoinColumn(name = "customer_account_id", insertable = false, updatable = false)
    private CustomerAccount customerAccount;

    @Column(name = "user_id")
    private Long userId;

    @Column(nullable = false)
    private String status = "PENDING";

    @Column(name = "total_amount", nullable = false)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "order_number")
    private String orderNumber;

    @Column(name = "payment_method")
    private String paymentMethod;

    @Column(name = "payment_status", nullable = false)
    private String paymentStatus = "PENDING";

    @Column(name = "stock_committed", nullable = false)
    private Boolean stockCommitted = false;

    private String notes;

    @Column(name = "buyer_name_snapshot")
    private String buyerNameSnapshot;

    @Column(name = "buyer_email_snapshot")
    private String buyerEmailSnapshot;

    @Column(name = "buyer_phone_snapshot")
    private String buyerPhoneSnapshot;

    @Column(name = "buyer_document_snapshot")
    private String buyerDocumentSnapshot;

    @Column(name = "buyer_person_type_snapshot")
    private String buyerPersonTypeSnapshot;

    @Column(name = "delivery_zip_code_snapshot")
    private String deliveryZipCodeSnapshot;

    @Column(name = "delivery_street_snapshot")
    private String deliveryStreetSnapshot;

    @Column(name = "delivery_number_snapshot")
    private String deliveryNumberSnapshot;

    @Column(name = "delivery_neighborhood_snapshot")
    private String deliveryNeighborhoodSnapshot;

    @Column(name = "delivery_city_snapshot")
    private String deliveryCitySnapshot;

    @Column(name = "delivery_state_snapshot")
    private String deliveryStateSnapshot;

    @Column(name = "updated_by_user_id")
    private Long updatedByUserId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    public void addItem(OrderItem item) {
        item.setOrder(this);
        items.add(item);
    }

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
