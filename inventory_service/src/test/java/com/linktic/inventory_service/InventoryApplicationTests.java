package com.linktic.inventory_service;

import com.linktic.inventory_service.domain.client.ProductsClient;
import com.linktic.inventory_service.infrastructure.mapper.InventoryMapper;
import com.linktic.inventory_service.infrastructure.persistence.jpa.InventoryJpaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(properties = {
        "spring.main.lazy-initialization=true"
})
@TestPropertySource(properties = {
        "spring.autoconfigure.exclude=" +
                "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration," +
                "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration," +
                "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration"
})
class InventoryApplicationTests {

    @MockitoBean
    private InventoryJpaRepository jpa;

    @MockitoBean
    private InventoryMapper mapper;

    @MockitoBean
    private ProductsClient productsClient;

    @Test
    void contextLoads() {
    }
}
