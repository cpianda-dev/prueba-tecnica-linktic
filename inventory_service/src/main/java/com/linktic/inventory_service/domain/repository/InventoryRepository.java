package com.linktic.inventory_service.domain.repository;

import com.linktic.inventory_service.domain.model.Inventory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface InventoryRepository {
    Inventory save(Inventory inventory);
    Optional<Inventory> findById(Long id);
    Optional<Inventory> findByProductId(Long productId);
    void deleteById(Long id);
    List<Inventory> findAll();
    Page<Inventory> findAllPaginatedList(Pageable pageable);
}
