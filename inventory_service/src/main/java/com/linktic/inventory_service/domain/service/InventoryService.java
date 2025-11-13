package com.linktic.inventory_service.domain.service;

import com.linktic.inventory_service.domain.client.ProductsClient;
import com.linktic.inventory_service.domain.model.Inventory;
import com.linktic.inventory_service.domain.model.InventoryDetails;
import com.linktic.inventory_service.domain.repository.InventoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@Service
@Transactional
public class InventoryService {
    private static final Logger log = LoggerFactory.getLogger(InventoryService.class);

    private final InventoryRepository repository;
    private final ProductsClient productsClient;
    private final boolean validateProducts;

    public InventoryService(InventoryRepository repository, Optional<ProductsClient> productsClientOpt) {
        this.repository = repository;
        this.productsClient = productsClientOpt.orElse(null);
        this.validateProducts = productsClientOpt.isPresent();
    }

    public Inventory create(Long productId, Integer quantity) {
        if (productId == null) throw new IllegalArgumentException("productId is required");
        if (quantity == null || quantity < 0) throw new IllegalArgumentException("quantity must be >= 0");

        if (validateProducts && productsClient != null && !productsClient.existsProduct(productId)) {
            throw new NoSuchElementException("Product not found.");
        }

        Inventory inv = new Inventory(null, productId, quantity, LocalDateTime.now(), null);
        Inventory saved = repository.save(inv);
        log.info("InventoryChanged event=CREATED productId={} newQuantity={}", saved.getProductId(), saved.getQuantity());

        return saved;
    }

    public Inventory get(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Inventory not found."));
    }

    public Inventory getByProductId(Long productId) {
        return repository.findByProductId(productId)
                .orElseThrow(() -> new NoSuchElementException("Inventory not found for productId " + productId));
    }

    public Inventory update(Long id, Integer quantity) {
        if (id == null) throw new IllegalArgumentException("id is required");
        Inventory current = get(id);
        if (quantity != null) {
            if (quantity < 0) throw new IllegalArgumentException("quantity must be >= 0");
            current.setQuantity(quantity);
        }
        current.setUpdatedAt(LocalDateTime.now());
        Inventory saved = repository.save(current);

        log.info("InventoryChanged event=UPDATED productId={} newQuantity={}", saved.getProductId(), saved.getQuantity());

        return saved;
    }

    public Inventory purchase(Long productId, Integer units) {
        if (productId == null) throw new IllegalArgumentException("productId is required");
        if (units == null || units <= 0) throw new IllegalArgumentException("units must be > 0");

        Inventory inv = getByProductId(productId);
        int newQty = inv.getQuantity() - units;
        if (newQty < 0) {
            throw new IllegalArgumentException("insufficient stock");
        }
        inv.setQuantity(newQty);
        inv.setUpdatedAt(LocalDateTime.now());
        Inventory saved = repository.save(inv);

        log.info("InventoryChanged event=PURCHASE productId={} delta=-{} newQuantity={}", productId, units, saved.getQuantity());

        return saved;
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }

    public List<Inventory> list() {
        return repository.findAll();
    }

    public Page<Inventory> paginatedList(int pageNumber, int pageSize) {
        int pn = Math.max(pageNumber, 1) - 1;
        int ps = Math.min(Math.max(pageSize, 1), 100);
        Pageable pageable = PageRequest.of(pn, ps);
        return repository.findAllPaginatedList(pageable);
    }

    public InventoryDetails getDetailsByProductId(Long productId) {
        if (productId == null) throw new IllegalArgumentException("productId is required");
        if (productsClient == null) {
            throw new IllegalStateException("Products integration is disabled; productsClient not configured");
        }
        var prod = productsClient.getProductSummary(productId);
        var inv = getByProductId(productId);
        return new InventoryDetails(inv, prod);
    }

}
