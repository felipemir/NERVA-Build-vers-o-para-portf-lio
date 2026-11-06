package com.nervatech.nerva_build.repository;

import com.nervatech.nerva_build.domain.model.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
}
