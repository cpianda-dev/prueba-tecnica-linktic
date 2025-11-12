package com.linktic.products_service.web.dto.jsonapi;

import com.linktic.products_service.domain.model.Product;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ProductDto {

    @NotBlank
    private String name;

    @NotNull
    @DecimalMin("0.01")
    private BigDecimal price;

    public static ProductDto from(Product product) {
        ProductDto model = new ProductDto();
        model.setName(product.getName());
        model.setPrice(product.getPrice());
        return model;
    }
}
