package com.linktic.inventory_service.web.controller;

import com.linktic.inventory_service.domain.client.ProductsClient;
import com.linktic.inventory_service.domain.model.Inventory;
import com.linktic.inventory_service.domain.repository.InventoryRepository;
import com.linktic.inventory_service.domain.service.InventoryService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatcher;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = InventoryController.class)
@AutoConfigureMockMvc(addFilters = false)
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class InventoryInternalIT {

    private static final MediaType JSON_API = MediaType.valueOf("application/vnd.api+json");

    @MockitoBean
    private InventoryRepository repository;

    @MockitoBean
    private ProductsClient productsClient;

    private final MockMvc mvc;

    InventoryInternalIT(MockMvc mvc) {
        this.mvc = mvc;
    }

    @TestConfiguration
    static class Cfg {
        @Bean
        InventoryService inventoryService(InventoryRepository repo, ProductsClient pc) {
            return new InventoryService(repo, Optional.of(pc));
        }
    }

    @Test
    void create_returns201_jsonapiEnvelope() throws Exception {
        when(productsClient.existsProduct(100L)).thenReturn(true);
        when(repository.save(any(Inventory.class))).thenAnswer(inv -> {
            Inventory i = inv.getArgument(0);
            i.setId(1L);
            return i;
        });

        String body = """
            {"data":{"type":"inventories","attributes":{"productId":100,"quantity":10}}}
        """;

        mvc.perform(post("/inventories")
                        .contentType(JSON_API).accept(JSON_API).content(body))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/inventories/1"))
                .andExpect(jsonPath("$.data.type").value("inventories"))
                .andExpect(jsonPath("$.data.id").value("1"))
                .andExpect(jsonPath("$.data.attributes.productId").value(100))
                .andExpect(jsonPath("$.data.attributes.quantity").value(10));

        verify(productsClient).existsProduct(100L);
        verify(repository).save(argThat(invMatches(100L, 10)));
    }

    @Test
    void get_byId_ok() throws Exception {
        var inv = new Inventory(5L, 777L, 9, LocalDateTime.now(), null);
        when(repository.findById(5L)).thenReturn(Optional.of(inv));

        mvc.perform(get("/inventories/{id}", 5L).accept(JSON_API))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.type").value("inventories"))
                .andExpect(jsonPath("$.data.id").value("5"))
                .andExpect(jsonPath("$.data.attributes.productId").value(777))
                .andExpect(jsonPath("$.data.attributes.quantity").value(9));
    }

    @Test
    void update_changesQuantity_ok() throws Exception {
        var inv = new Inventory(10L, 300L, 1, LocalDateTime.now(), null);
        when(repository.findById(10L)).thenReturn(Optional.of(inv));
        when(repository.save(any(Inventory.class))).thenAnswer(a -> a.getArgument(0));

        String body = """
            {"data":{"type":"inventories","attributes":{"quantity":25}}}
        """;

        mvc.perform(put("/inventories/{id}", 10L)
                        .contentType(JSON_API).accept(JSON_API).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.attributes.quantity").value(25));
    }

    @Test
    void purchase_insufficientStock_400_jsonapi() throws Exception {
        var inv = new Inventory(9L, 1234L, 1, LocalDateTime.now(), null);
        when(repository.findByProductId(1234L)).thenReturn(Optional.of(inv));

        String body = """
            {"data":{"type":"inventories","attributes":{"productId":1234,"units":5}}}
        """;

        mvc.perform(post("/inventories/purchase")
                        .contentType(JSON_API).accept(JSON_API).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].title").value("Bad Request"))
                .andExpect(jsonPath("$.errors[0].detail").value("insufficient stock"));

        verify(repository, never()).save(any());
    }

    @Test
    void paginated_ok_linksAndMeta() throws Exception {
        var inv = new Inventory(1L, 42L, 3, LocalDateTime.now(), null);
        Page<Inventory> page = new PageImpl<>(List.of(inv), PageRequest.of(0, 10), 1);
        when(repository.findAllPaginatedList(any())).thenReturn(page);

        mvc.perform(get("/inventories/paginated")
                        .param("pageNumber", "1")
                        .param("pageSize", "10")
                        .accept(JSON_API))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value("1"))
                .andExpect(jsonPath("$.data[0].attributes.productId").value(42))
                .andExpect(jsonPath("$.meta.totalElements").value(1))
                .andExpect(jsonPath("$.meta.totalPages").value(1))
                .andExpect(jsonPath("$.meta.pageNumber").value(1))
                .andExpect(jsonPath("$.meta.pageSize").value(10));
    }

    @Test
    void details_ok_usesProductsClientMock() throws Exception {
        var inv = new Inventory(7L, 700L, 9, LocalDateTime.now(), null);
        when(repository.findByProductId(700L)).thenReturn(Optional.of(inv));

        var summary = new ProductsClient.ProductSummary(700L, "Mouse", new BigDecimal("19.99"));
        when(productsClient.getProductSummary(700L)).thenReturn(summary);

        mvc.perform(get("/inventories/product/{productId}", 700L).accept(JSON_API))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("7"))
                .andExpect(jsonPath("$.data.attributes.productId").value(700))
                .andExpect(jsonPath("$.data.attributes.quantity").value(9))
                .andExpect(jsonPath("$.data.attributes.product.id").value(700))
                .andExpect(jsonPath("$.data.attributes.product.name").value("Mouse"))
                .andExpect(jsonPath("$.data.attributes.product.price").value(19.99))
                .andExpect(jsonPath("$.links.self").value("/inventories/product/700"));
    }

    private static ArgumentMatcher<Inventory> invMatches(Long expectedProductId, Integer expectedQty) {
        return inv -> inv != null
                && (expectedProductId == null || expectedProductId.equals(inv.getProductId()))
                && (expectedQty == null || expectedQty.equals(inv.getQuantity()));
    }
}
