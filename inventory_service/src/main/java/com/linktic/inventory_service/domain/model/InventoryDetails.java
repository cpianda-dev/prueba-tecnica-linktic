package com.linktic.inventory_service.domain.model;

import com.linktic.inventory_service.domain.client.ProductsClient;

public record InventoryDetails(
        Inventory inventory,
        ProductsClient.ProductSummary product
) {}
