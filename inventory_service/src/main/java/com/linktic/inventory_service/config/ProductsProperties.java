package com.linktic.inventory_service.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "inventory.products")
@Getter @Setter
public class ProductsProperties {
    private String baseUrl;
    private ApiKey apiKey = new ApiKey();
    @Getter
    @Setter
    public static class ApiKey {
        private String header;
        private String value;
    }
}
