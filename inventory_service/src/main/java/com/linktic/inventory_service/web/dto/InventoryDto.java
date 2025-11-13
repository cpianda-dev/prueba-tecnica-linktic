package com.linktic.inventory_service.web.dto;

import com.linktic.inventory_service.domain.model.Inventory;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class InventoryDto {

    @NotNull
    private Long productId;

    @NotNull @Min(0)
    private Integer quantity;

    public static InventoryDto from(Inventory inv) {
        InventoryDto dto = new InventoryDto();
        dto.setProductId(inv.getProductId());
        dto.setQuantity(inv.getQuantity());
        return dto;
    }
}
