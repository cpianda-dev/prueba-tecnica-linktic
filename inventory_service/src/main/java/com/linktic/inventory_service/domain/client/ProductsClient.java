package com.linktic.inventory_service.domain.client;

import java.math.BigDecimal;

public interface ProductsClient {
    boolean existsProduct(Long productId);

    ProductSummary getProductSummary(Long productId);

    record ProductSummary(Long id, String name, BigDecimal price) {}
}