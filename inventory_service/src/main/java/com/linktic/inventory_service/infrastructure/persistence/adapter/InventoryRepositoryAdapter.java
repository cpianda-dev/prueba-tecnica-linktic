package com.linktic.inventory_service.infrastructure.persistence.adapter;

import com.linktic.inventory_service.domain.model.Inventory;
import com.linktic.inventory_service.domain.repository.InventoryRepository;
import com.linktic.inventory_service.infrastructure.mapper.InventoryMapper;
import com.linktic.inventory_service.infrastructure.persistence.entity.InventoryEntity;
import com.linktic.inventory_service.infrastructure.persistence.jpa.InventoryJpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class InventoryRepositoryAdapter implements InventoryRepository {
    private final InventoryJpaRepository jpa;
    private final InventoryMapper mapper;

    public InventoryRepositoryAdapter(InventoryJpaRepository jpa, InventoryMapper mapper) {
        this.jpa = jpa;
        this.mapper = mapper;
    }

    @Override
    public Inventory save(Inventory inventory) {
        InventoryEntity saved = jpa.save(mapper.toEntity(inventory));
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Inventory> findById(Long id) {
        return jpa.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Inventory> findByProductId(Long productId) {
        return jpa.findByProductId(productId).map(mapper::toDomain);
    }

    @Override
    public void deleteById(Long id) {
        jpa.deleteById(id);
    }

    @Override
    public List<Inventory> findAll() {
        return jpa.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public Page<Inventory> findAllPaginatedList(Pageable pageable) {
        return jpa.findAll(pageable).map(mapper::toDomain);
    }

}
