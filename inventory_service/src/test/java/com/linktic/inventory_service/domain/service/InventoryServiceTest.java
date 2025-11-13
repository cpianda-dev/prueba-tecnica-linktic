package com.linktic.inventory_service.domain.service;

import com.linktic.inventory_service.domain.client.ProductsClient;
import com.linktic.inventory_service.domain.model.Inventory;
import com.linktic.inventory_service.domain.repository.InventoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InventoryServiceTest {

    private InventoryRepository repository;
    private ProductsClient productsClient;
    private InventoryService service;
    private InventoryService serviceWithClient;

    @BeforeEach
    void setUp() {
        repository = mock(InventoryRepository.class);
        productsClient = mock(ProductsClient.class);
        service = new InventoryService(repository, Optional.empty());
        serviceWithClient = new InventoryService(repository, Optional.of(productsClient));
    }

    @Test
    void create_shouldCallProductsAndSave_whenProductExists() {
        when(productsClient.existsProduct(100L)).thenReturn(true);
        when(repository.save(any())).thenAnswer(inv -> {
            Inventory i = inv.getArgument(0);
            i.setId(1L);
            return i;
        });

        Inventory created = serviceWithClient.create(100L, 10);

        assertEquals(1L, created.getId());
        verify(productsClient).existsProduct(100L);
        verify(repository).save(any());
    }

    @Test
    void create_shouldThrow404_whenProductNotExists() {
        when(productsClient.existsProduct(100L)).thenReturn(false);
        assertThrows(NoSuchElementException.class, () -> serviceWithClient.create(100L, 1));
        verify(repository, never()).save(any());
    }

    @Test
    void create_shouldSaveInventory_whenValid_withoutRemoteValidation() {
        when(repository.save(any(Inventory.class))).thenAnswer(inv -> {
            Inventory i = inv.getArgument(0);
            i.setId(1L);
            return i;
        });

        Inventory created = service.create(100L, 10);

        assertNotNull(created);
        assertEquals(1L, created.getId());
        assertEquals(100L, created.getProductId());
        assertEquals(10, created.getQuantity());
        verify(repository, times(1)).save(any(Inventory.class));
    }

    @Test
    void create_shouldThrow_whenInvalidInputs() {
        assertThrows(IllegalArgumentException.class, () -> service.create(null, 10));
        assertThrows(IllegalArgumentException.class, () -> service.create(100L, null));
        assertThrows(IllegalArgumentException.class, () -> service.create(100L, -1));
        verify(repository, never()).save(any());
    }

    @Test
    void get_shouldReturnInventory_whenExists() {
        Inventory inv = new Inventory(1L, 100L, 5, LocalDateTime.now(), null);
        when(repository.findById(1L)).thenReturn(Optional.of(inv));

        Inventory result = service.get(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(100L, result.getProductId());
    }

    @Test
    void get_shouldThrow_whenNotExists() {
        when(repository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(NoSuchElementException.class, () -> service.get(1L));
    }

    @Test
    void getByProductId_shouldReturn_whenExists() {
        Inventory inv = new Inventory(5L, 777L, 9, LocalDateTime.now(), null);
        when(repository.findByProductId(777L)).thenReturn(Optional.of(inv));

        Inventory result = service.getByProductId(777L);

        assertNotNull(result);
        assertEquals(5L, result.getId());
        assertEquals(777L, result.getProductId());
        assertEquals(9, result.getQuantity());
    }

    @Test
    void getByProductId_shouldThrow_whenNotExists() {
        when(repository.findByProductId(777L)).thenReturn(Optional.empty());
        NoSuchElementException ex = assertThrows(NoSuchElementException.class, () -> service.getByProductId(777L));
        assertThat(ex.getMessage()).contains("Inventory not found for productId 777");
    }

    @Test
    void update_shouldModifyQuantity_whenValid() {
        Inventory inv = new Inventory(1L, 100L, 0, LocalDateTime.now(), null);
        when(repository.findById(1L)).thenReturn(Optional.of(inv));
        when(repository.save(any(Inventory.class))).thenAnswer(i -> i.getArgument(0));

        Inventory updated = service.update(1L, 10);

        assertEquals(10, updated.getQuantity());
        assertNotNull(updated.getUpdatedAt());
        verify(repository).save(inv);
    }

    @Test
    void update_shouldThrow_whenQuantityNegative() {
        Inventory inv = new Inventory(1L, 100L, 1, LocalDateTime.now(), null);
        when(repository.findById(1L)).thenReturn(Optional.of(inv));
        assertThrows(IllegalArgumentException.class, () -> service.update(1L, -1));
        verify(repository, never()).save(any());
    }

    @Test
    void purchase_shouldDecreaseQuantity_andSave_whenValid() {
        Inventory inv = new Inventory(10L, 100L, 10, LocalDateTime.now(), null);
        when(repository.findByProductId(100L)).thenReturn(Optional.of(inv));
        when(repository.save(any(Inventory.class))).thenAnswer(i -> i.getArgument(0));

        Inventory updated = service.purchase(100L, 3);

        assertEquals(7, updated.getQuantity());
        assertNotNull(updated.getUpdatedAt());

        ArgumentCaptor<Inventory> captor = ArgumentCaptor.forClass(Inventory.class);
        verify(repository).save(captor.capture());
        Inventory saved = captor.getValue();
        assertEquals(100L, saved.getProductId());
        assertEquals(7, saved.getQuantity());
    }

    @Test
    void purchase_shouldThrow_whenProductIdNull() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.purchase(null, 1));
        assertThat(ex.getMessage()).contains("productId is required");
    }

    @Test
    void purchase_shouldThrow_whenUnitsNullOrNotPositive() {
        assertThrows(IllegalArgumentException.class, () -> service.purchase(100L, null));
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.purchase(100L, 0));
        assertThat(ex.getMessage()).contains("units must be > 0");
    }

    @Test
    void purchase_shouldThrow_whenInventoryNotFound() {
        when(repository.findByProductId(100L)).thenReturn(Optional.empty());
        NoSuchElementException ex = assertThrows(NoSuchElementException.class, () -> service.purchase(100L, 1));
        assertThat(ex.getMessage()).contains("Inventory not found for productId 100");
    }

    @Test
    void purchase_shouldThrow_whenInsufficientStock() {
        Inventory inv = new Inventory(10L, 100L, 2, LocalDateTime.now(), null);
        when(repository.findByProductId(100L)).thenReturn(Optional.of(inv));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.purchase(100L, 3));
        assertThat(ex.getMessage()).contains("insufficient stock");
        verify(repository, never()).save(any());
    }

    @Test
    void delete_shouldCallRepository() {
        doNothing().when(repository).deleteById(1L);
        service.delete(1L);
        verify(repository).deleteById(1L);
    }

    @Test
    void list_shouldReturnAllInventories() {
        List<Inventory> inventories = List.of(
                new Inventory(1L, 100L, 1, LocalDateTime.now(), null),
                new Inventory(2L, 200L, 2, LocalDateTime.now(), null)
        );
        when(repository.findAll()).thenReturn(inventories);

        List<Inventory> result = service.list();

        assertThat(result).hasSize(2)
                .extracting(Inventory::getProductId)
                .containsExactlyInAnyOrder(100L, 200L);
    }

    @Test
    void paginatedList_shouldReturnPage() {
        List<Inventory> inventories = List.of(
                new Inventory(1L, 100L, 1, LocalDateTime.now(), null)
        );
        Page<Inventory> page = new PageImpl<>(inventories);
        when(repository.findAllPaginatedList(PageRequest.of(0, 10))).thenReturn(page);

        Page<Inventory> result = service.paginatedList(1, 10);

        assertThat(result.getContent()).hasSize(1);
        assertEquals(100L, result.getContent().get(0).getProductId());
    }

    @Test
    void getDetailsByProductId_shouldReturn_inventoryAndProduct() {
        Inventory inv = new Inventory(50L, 555L, 8, LocalDateTime.now(), null);
        when(repository.findByProductId(555L)).thenReturn(Optional.of(inv));
        when(productsClient.getProductSummary(555L))
                .thenReturn(new ProductsClient.ProductSummary(555L, "Laptop", new BigDecimal("1234.50")));

        var details = serviceWithClient.getDetailsByProductId(555L);

        assertNotNull(details);
        assertEquals(50L, details.inventory().getId());
        assertEquals(555L, details.inventory().getProductId());
        assertEquals(8, details.inventory().getQuantity());

        assertEquals(555L, details.product().id());
        assertEquals("Laptop", details.product().name());
        assertEquals(new BigDecimal("1234.50"), details.product().price());

        verify(productsClient).getProductSummary(555L);
        verify(repository).findByProductId(555L);
    }

    @Test
    void getDetailsByProductId_shouldThrow_whenProductIdNull() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> serviceWithClient.getDetailsByProductId(null));
        assertThat(ex.getMessage()).contains("productId is required");
    }

    @Test
    void getDetailsByProductId_shouldBubbleUp_whenInventoryMissing() {
        when(productsClient.getProductSummary(999L))
                .thenReturn(new ProductsClient.ProductSummary(999L, "X", new BigDecimal("1.00")));
        when(repository.findByProductId(999L)).thenReturn(Optional.empty());

        NoSuchElementException ex = assertThrows(NoSuchElementException.class, () -> serviceWithClient.getDetailsByProductId(999L));
        assertThat(ex.getMessage()).contains("Inventory not found for productId 999");
    }

    @Test
    void getDetailsByProductId_shouldBubbleUp_whenProductServiceSaysNotFound() {
        when(productsClient.getProductSummary(321L)).thenThrow(new NoSuchElementException("Product not found."));
        NoSuchElementException ex = assertThrows(NoSuchElementException.class, () -> serviceWithClient.getDetailsByProductId(321L));
        assertThat(ex.getMessage()).contains("Product not found.");
        verify(repository, never()).findByProductId(anyLong());
    }
}