package com.nervatech.nerva_build.service;

import com.nervatech.nerva_build.domain.model.Product;
import com.nervatech.nerva_build.domain.model.Order;
import com.nervatech.nerva_build.domain.model.OrderItem;
import com.nervatech.nerva_build.dto.ApiFieldErrorDTO;
import com.nervatech.nerva_build.exception.ApiValidationException;
import com.nervatech.nerva_build.repository.ProductRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class StockService {

    private final ProductRepository productRepository;

    public StockService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public void validateAvailableForOrder(Long storeId, Product product, Integer requestedQuantity, String fieldName) {
        if (product == null) {
            throw new ApiValidationException("Erro de validacao", List.of(new ApiFieldErrorDTO(
                    fieldName,
                    "Produto nao encontrado"
            )));
        }

        if (product.getStore() == null || !storeId.equals(product.getStore().getId())) {
            throw new ApiValidationException("Erro de validacao", List.of(new ApiFieldErrorDTO(
                    fieldName,
                    "Produto indisponivel para esta loja"
            )));
        }

        if (requestedQuantity == null || requestedQuantity <= 0) {
            throw new ApiValidationException("Erro de validacao", List.of(new ApiFieldErrorDTO(
                    fieldName,
                    "Quantidade deve ser maior que zero"
            )));
        }

        BigDecimal availableQuantity = product.getStockQuantity();
        if (availableQuantity == null) {
            throw new ApiValidationException("Erro de validacao", List.of(new ApiFieldErrorDTO(
                    fieldName,
                    "Estoque indisponivel para o produto " + product.getName()
            )));
        }

        BigDecimal requested = BigDecimal.valueOf(requestedQuantity);
        if (availableQuantity.compareTo(requested) < 0) {
            throw new ApiValidationException("Erro de validacao", List.of(new ApiFieldErrorDTO(
                    fieldName,
                    "Estoque insuficiente para o produto " + product.getName()
                            + ". Disponivel: " + availableQuantity.stripTrailingZeros().toPlainString()
                            + ", solicitado: " + requestedQuantity
            )));
        }
    }

    public void commitStock(Order order) {
        for (Map.Entry<Long, Integer> entry : aggregateQuantities(order).entrySet()) {
            Product product = productRepository.findWithLockByIdAndStore_Id(entry.getKey(), order.getStoreId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Produto indisponivel para confirmacao do pedido"));

            validateAvailableForOrder(order.getStoreId(), product, entry.getValue(), "items.productId");
            product.setStockQuantity(product.getStockQuantity().subtract(BigDecimal.valueOf(entry.getValue())));
            productRepository.save(product);
        }
    }

    public void restoreStock(Order order) {
        for (Map.Entry<Long, Integer> entry : aggregateQuantities(order).entrySet()) {
            Product product = productRepository.findWithLockByIdAndStore_Id(entry.getKey(), order.getStoreId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Produto indisponivel para restauracao do estoque"));

            BigDecimal currentStock = product.getStockQuantity() == null ? BigDecimal.ZERO : product.getStockQuantity();
            product.setStockQuantity(currentStock.add(BigDecimal.valueOf(entry.getValue())));
            productRepository.save(product);
        }
    }

    private Map<Long, Integer> aggregateQuantities(Order order) {
        Map<Long, Integer> quantitiesByProduct = new LinkedHashMap<>();
        for (OrderItem item : order.getItems()) {
            quantitiesByProduct.merge(item.getProductId(), item.getQuantity(), Integer::sum);
        }
        return quantitiesByProduct;
    }
}
