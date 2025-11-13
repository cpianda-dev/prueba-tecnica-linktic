package com.linktic.inventory_service.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI inventoryApi() {
        final String apiKeyName = "X-API-Key";
        return new OpenAPI()
                .info(new Info().title("Inventory API").version("v1").description("JSON:API compliant"))
                .addSecurityItem(new SecurityRequirement().addList(apiKeyName))
                .components(new Components().addSecuritySchemes(apiKeyName,
                        new SecurityScheme().name(apiKeyName).type(SecurityScheme.Type.APIKEY).in(SecurityScheme.In.HEADER)));
    }
}

