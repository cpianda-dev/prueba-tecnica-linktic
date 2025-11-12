package com.linktic.inventory_service.infrastructure.persistence.adapter;

import com.linktic.inventory_service.domain.model.Inventory;
import com.linktic.inventory_service.infrastructure.mapper.InventoryMapper;
import com.linktic.inventory_service.infrastructure.persistence.entity.InventoryEntity;
import com.linktic.inventory_service.infrastructure.persistence.jpa.InventoryJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InventoryRepositoryAdapterTest {

    private InventoryJpaRepository jpa;
    private InventoryMapper mapper;
    private InventoryRepositoryAdapter repository;

    @BeforeEach
    void setUp() {
        jpa = mock(InventoryJpaRepository.class);
        mapper = mock(InventoryMapper.class);
        repository = new InventoryRepositoryAdapter(jpa, mapper);
    }

    @Test
    void save_shouldCallJpaAndMapper() {
        Inventory domain = new Inventory(null, 100L, 10, LocalDateTime.now(), null);
        InventoryEntity entity = new InventoryEntity(null, 100L, 10, LocalDateTime.now(), null);
        InventoryEntity savedEntity = new InventoryEntity(1L, 100L, 10, LocalDateTime.now(), null);
        Inventory savedDomain = new Inventory(1L, 100L, 10, LocalDateTime.now(), null);

        when(mapper.toEntity(domain)).thenReturn(entity);
        when(jpa.save(entity)).thenReturn(savedEntity);
        when(mapper.toDomain(savedEntity)).thenReturn(savedDomain);

        Inventory result = repository.save(domain);

        assertThat(result.getId()).isEqualTo(1L);
        verify(jpa).save(entity);
        verify(mapper).toDomain(savedEntity);
    }

    @Test
    void findById_shouldReturnDomain() {
        InventoryEntity entity = new InventoryEntity(1L, 100L, 10, LocalDateTime.now(), null);
        Inventory domain = new Inventory(1L, 100L, 10, LocalDateTime.now(), null);

        when(jpa.findById(1L)).thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(domain);

        Optional<Inventory> result = repository.findById(1L);

        assertThat(result).isPresent().contains(domain);
    }

    @Test
    void findById_shouldReturnEmpty() {
        when(jpa.findById(1L)).thenReturn(Optional.empty());
        Optional<Inventory> result = repository.findById(1L);
        assertThat(result).isEmpty();
    }

    @Test
    void findByProductId_shouldReturnDomain() {
        InventoryEntity entity = new InventoryEntity(5L, 777L, 9, LocalDateTime.now(), null);
        Inventory domain = new Inventory(5L, 777L, 9, LocalDateTime.now(), null);

        when(jpa.findByProductId(777L)).thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(domain);

        Optional<Inventory> result = repository.findByProductId(777L);

        assertThat(result).isPresent().contains(domain);
        verify(jpa).findByProductId(777L);
        verify(mapper).toDomain(entity);
    }

    @Test
    void findByProductId_shouldReturnEmpty() {
        when(jpa.findByProductId(888L)).thenReturn(Optional.empty());

        Optional<Inventory> result = repository.findByProductId(888L);

        assertThat(result).isEmpty();
        verify(jpa).findByProductId(888L);
        verify(mapper, never()).toDomain(any());
    }

    @Test
    void deleteById_shouldCallJpa() {
        doNothing().when(jpa).deleteById(1L);
        repository.deleteById(1L);
        verify(jpa).deleteById(1L);
    }

    @Test
    void findAll_shouldReturnMappedList() {
        InventoryEntity e1 = new InventoryEntity(1L, 100L, 10, LocalDateTime.now(), null);
        InventoryEntity e2 = new InventoryEntity(2L, 200L, 20, LocalDateTime.now(), null);
        Inventory d1 = new Inventory(1L, 100L, 10, LocalDateTime.now(), null);
        Inventory d2 = new Inventory(2L, 200L, 20, LocalDateTime.now(), null);

        when(jpa.findAll()).thenReturn(List.of(e1, e2));
        when(mapper.toDomain(e1)).thenReturn(d1);
        when(mapper.toDomain(e2)).thenReturn(d2);

        List<Inventory> result = repository.findAll();

        assertThat(result).containsExactly(d1, d2);
    }

    @Test
    void findAllPaginatedList_shouldReturnMappedPage() {
        InventoryEntity e1 = new InventoryEntity(1L, 100L, 10, LocalDateTime.now(), null);
        InventoryEntity e2 = new InventoryEntity(2L, 200L, 20, LocalDateTime.now(), null);
        Inventory d1 = new Inventory(1L, 100L, 10, LocalDateTime.now(), null);
        Inventory d2 = new Inventory(2L, 200L, 20, LocalDateTime.now(), null);

        Pageable pageable = PageRequest.of(0, 2);
        Page<InventoryEntity> page = new PageImpl<>(List.of(e1, e2));
        when(jpa.findAll(pageable)).thenReturn(page);
        when(mapper.toDomain(e1)).thenReturn(d1);
        when(mapper.toDomain(e2)).thenReturn(d2);

        Page<Inventory> result = repository.findAllPaginatedList(pageable);

        assertThat(result.getContent()).containsExactly(d1, d2);
    }
}
