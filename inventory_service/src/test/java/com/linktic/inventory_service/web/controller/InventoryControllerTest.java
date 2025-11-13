package com.linktic.inventory_service.web.controller;

import com.linktic.inventory_service.domain.client.ProductsClient;
import com.linktic.inventory_service.domain.model.Inventory;
import com.linktic.inventory_service.domain.service.InventoryService;
import com.linktic.inventory_service.web.dto.PurchaseDto;
import com.linktic.inventory_service.web.dto.jsonapi.JsonApiData;
import com.linktic.inventory_service.web.dto.jsonapi.JsonApiRequest;
import com.linktic.inventory_service.web.dto.InventoryDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.linktic.inventory_service.web.handler.RestExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = InventoryController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(RestExceptionHandler.class)
class InventoryControllerTest {

    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper;

    @MockitoBean
    private InventoryService service;

    @Autowired
    InventoryControllerTest(MockMvc mockMvc, ObjectMapper objectMapper) {
        this.mockMvc = mockMvc;
        this.objectMapper = objectMapper;
    }

    @BeforeEach
    void resetMocks() {
        reset(service);
    }

    // -------------------------
    // CREATE
    // -------------------------
    @Test
    void create_shouldReturnCreatedInventory() throws Exception {
        InventoryDto dto = new InventoryDto(100L, 10);
        JsonApiRequest<InventoryDto> req = new JsonApiRequest<>();
        req.setData(new JsonApiData<>("inventories", null, dto));

        Inventory created = new Inventory(1L, 100L, 10, LocalDateTime.now(), null);
        when(service.create(dto.getProductId(), dto.getQuantity())).thenReturn(created);

        mockMvc.perform(post("/inventories")
                        .contentType("application/vnd.api+json")
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value("1"))
                .andExpect(jsonPath("$.data.type").value("inventories"))
                .andExpect(jsonPath("$.data.attributes.productId").value(100))
                .andExpect(jsonPath("$.data.attributes.quantity").value(10));
    }

    @Test
    void create_shouldReturn400_whenInvalidQuantity() throws Exception {
        InventoryDto dto = new InventoryDto(100L, -1);
        JsonApiRequest<InventoryDto> req = new JsonApiRequest<>();
        req.setData(new JsonApiData<>("inventories", null, dto));

        when(service.create(dto.getProductId(), dto.getQuantity()))
                .thenThrow(new IllegalArgumentException("quantity must be >= 0"));

        mockMvc.perform(post("/inventories")
                        .contentType("application/vnd.api+json")
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].status").value("400"))
                .andExpect(jsonPath("$.errors[0].title").value("Bad Request"))
                .andExpect(jsonPath("$.errors[0].detail").value("quantity must be >= 0"));
    }

    @Test
    void purchase_shouldReturn200_andUpdatedInventory() throws Exception {
        long productId = 777L;
        PurchaseDto dto = new PurchaseDto(productId, 3); // restar 3 unidades

        JsonApiRequest<PurchaseDto> req = new JsonApiRequest<>();
        req.setData(new JsonApiData<>("inventories", null, dto));

        // inventario resultante luego de la compra
        Inventory updated = new Inventory(5L, productId, 7, LocalDateTime.now(), LocalDateTime.now());
        when(service.purchase(productId, 3)).thenReturn(updated);

        mockMvc.perform(post("/inventories/purchase")
                        .contentType("application/vnd.api+json")
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("5"))
                .andExpect(jsonPath("$.data.type").value("inventories"))
                .andExpect(jsonPath("$.data.attributes.productId").value((int) productId))
                .andExpect(jsonPath("$.data.attributes.quantity").value(7))
                .andExpect(jsonPath("$.links.self").value("/inventories/product/" + productId));
    }

    @Test
    void purchase_shouldReturn400_whenUnitsInvalid() throws Exception {
        long productId = 10L;
        PurchaseDto dto = new PurchaseDto(productId, 0);

        JsonApiRequest<PurchaseDto> req = new JsonApiRequest<>();
        req.setData(new JsonApiData<>("inventories", null, dto));

        when(service.purchase(productId, 0))
                .thenThrow(new IllegalArgumentException("units must be > 0"));

        mockMvc.perform(post("/inventories/purchase")
                        .contentType("application/vnd.api+json")
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].status").value("400"))
                .andExpect(jsonPath("$.errors[0].title").value("Bad Request"))
                .andExpect(jsonPath("$.errors[0].detail").value("units must be > 0"));
    }

    @Test
    void purchase_shouldReturn400_whenInsufficientStock() throws Exception {
        long productId = 22L;
        PurchaseDto dto = new PurchaseDto(productId, 1000);

        JsonApiRequest<PurchaseDto> req = new JsonApiRequest<>();
        req.setData(new JsonApiData<>("inventories", null, dto));

        when(service.purchase(productId, 1000))
                .thenThrow(new IllegalArgumentException("insufficient stock"));

        mockMvc.perform(post("/inventories/purchase")
                        .contentType("application/vnd.api+json")
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].status").value("400"))
                .andExpect(jsonPath("$.errors[0].detail").value("insufficient stock"));
    }

    // -------------------------
    // GET
    // -------------------------
    @Test
    void get_shouldReturnInventory_whenExists() throws Exception {
        Inventory inv = new Inventory(1L, 100L, 10, LocalDateTime.now(), null);
        when(service.get(1L)).thenReturn(inv);

        mockMvc.perform(get("/inventories/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("1"))
                .andExpect(jsonPath("$.data.attributes.productId").value(100))
                .andExpect(jsonPath("$.data.attributes.quantity").value(10));
    }

    @Test
    void get_shouldReturn404_whenInventoryNotFound() throws Exception {
        when(service.get(1L)).thenThrow(new NoSuchElementException("Inventory not found."));

        mockMvc.perform(get("/inventories/1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errors[0].status").value("404"))
                .andExpect(jsonPath("$.errors[0].title").value("Not Found"))
                .andExpect(jsonPath("$.errors[0].detail").value("Inventory not found."));
    }

    @Test
    void getByProductId_shouldReturnInventory() throws Exception {
        Inventory inv = new Inventory(5L, 777L, 9, LocalDateTime.now(), null);
        var prod = new ProductsClient.ProductSummary(
                777L, "Laptop", new java.math.BigDecimal("1999.99")
        );

        when(service.getDetailsByProductId(777L))
                .thenReturn(new com.linktic.inventory_service.domain.model.InventoryDetails(inv, prod));

        mockMvc.perform(get("/inventories/product/777"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("5"))
                .andExpect(jsonPath("$.data.type").value("inventories"))
                .andExpect(jsonPath("$.data.attributes.productId").value(777))
                .andExpect(jsonPath("$.data.attributes.quantity").value(9))
                .andExpect(jsonPath("$.data.attributes.product.id").value(777))
                .andExpect(jsonPath("$.data.attributes.product.name").value("Laptop"))
                .andExpect(jsonPath("$.data.attributes.product.price").value(1999.99))
                .andExpect(jsonPath("$.links.self").value("/inventories/product/777"));
    }

    // -------------------------
    // UPDATE
    // -------------------------
    @Test
    void update_shouldReturnUpdatedInventory() throws Exception {
        InventoryDto dto = new InventoryDto(100L, 25);
        JsonApiRequest<InventoryDto> req = new JsonApiRequest<>();
        req.setData(new JsonApiData<>("inventories", null, dto));

        Inventory updated = new Inventory(1L, 100L, 25, LocalDateTime.now(), LocalDateTime.now());
        when(service.update(1L, dto.getQuantity())).thenReturn(updated);

        mockMvc.perform(put("/inventories/1")
                        .contentType("application/vnd.api+json")
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("1"))
                .andExpect(jsonPath("$.data.attributes.productId").value(100))
                .andExpect(jsonPath("$.data.attributes.quantity").value(25));
    }

    @Test
    void update_shouldReturn404_whenInventoryNotFound() throws Exception {
        InventoryDto dto = new InventoryDto(100L, 25);
        JsonApiRequest<InventoryDto> req = new JsonApiRequest<>();
        req.setData(new JsonApiData<>("inventories", null, dto));

        when(service.update(1L, dto.getQuantity()))
                .thenThrow(new NoSuchElementException("Inventory not found."));

        mockMvc.perform(put("/inventories/1")
                        .contentType("application/vnd.api+json")
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errors[0].status").value("404"))
                .andExpect(jsonPath("$.errors[0].detail").value("Inventory not found."));
    }

    @Test
    void update_shouldReturn400_whenQuantityInvalid() throws Exception {
        InventoryDto dto = new InventoryDto(100L, -5);
        JsonApiRequest<InventoryDto> req = new JsonApiRequest<>();
        req.setData(new JsonApiData<>("inventories", null, dto));

        when(service.update(1L, dto.getQuantity()))
                .thenThrow(new IllegalArgumentException("quantity must be >= 0"));

        mockMvc.perform(put("/inventories/1")
                        .contentType("application/vnd.api+json")
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].status").value("400"))
                .andExpect(jsonPath("$.errors[0].detail").value("quantity must be >= 0"));
    }

    // -------------------------
    // DELETE
    // -------------------------
    @Test
    void delete_shouldReturnNoContent() throws Exception {
        doNothing().when(service).delete(1L);

        mockMvc.perform(delete("/inventories/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void delete_shouldReturn404_whenInventoryNotFound() throws Exception {
        doThrow(new NoSuchElementException("Inventory not found.")).when(service).delete(1L);

        mockMvc.perform(delete("/inventories/1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errors[0].status").value("404"));
    }

    // -------------------------
    // LIST
    // -------------------------
    @Test
    void list_shouldReturnInventoryList() throws Exception {
        Inventory i1 = new Inventory(1L, 100L, 10, LocalDateTime.now(), null);
        Inventory i2 = new Inventory(2L, 200L, 20, LocalDateTime.now(), null);
        when(service.list()).thenReturn(List.of(i1, i2));

        mockMvc.perform(get("/inventories/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].attributes.productId").value(100))
                .andExpect(jsonPath("$.data[1].attributes.quantity").value(20));
    }

    // -------------------------
    // PAGINATED
    // -------------------------
    @Test
    void paginatedList_shouldReturnPage() throws Exception {
        Inventory i1 = new Inventory(1L, 100L, 10, LocalDateTime.now(), null);
        Inventory i2 = new Inventory(2L, 200L, 20, LocalDateTime.now(), null);
        Page<Inventory> page = new PageImpl<>(List.of(i1, i2), PageRequest.of(0, 2), 5);
        when(service.paginatedList(1, 2)).thenReturn(page);

        mockMvc.perform(get("/inventories/paginated?pageNumber=1&pageSize=2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.meta.totalElements").value(5))
                .andExpect(jsonPath("$.meta.totalPages").value(3))
                .andExpect(jsonPath("$.links.self").exists())
                .andExpect(jsonPath("$.links.first").exists())
                .andExpect(jsonPath("$.links.last").exists());
    }

}
