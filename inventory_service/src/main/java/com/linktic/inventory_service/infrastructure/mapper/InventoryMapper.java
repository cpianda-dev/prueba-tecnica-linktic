package com.linktic.inventory_service.infrastructure.mapper;

import com.linktic.inventory_service.domain.model.Inventory;
import com.linktic.inventory_service.infrastructure.persistence.entity.InventoryEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface InventoryMapper {
    Inventory toDomain(InventoryEntity entity);

    @Mapping(target = "id", source = "id")
    InventoryEntity toEntity(Inventory domain);
}
