package com.linktic.products_service.web.controller;

import com.linktic.products_service.domain.model.Product;
import com.linktic.products_service.domain.service.ProductService;
import com.linktic.products_service.web.dto.jsonapi.JsonApiData;
import com.linktic.products_service.web.dto.jsonapi.JsonApiRequest;
import com.linktic.products_service.web.dto.jsonapi.ProductDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.linktic.products_service.web.handler.RestExceptionHandler;
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

import java.math.BigDecimal;
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

@WebMvcTest(controllers = ProductController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(RestExceptionHandler.class)
class ProductControllerTest {

    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper;

    @MockitoBean
    private ProductService service;

    private static final String API_KEY_HEADER = "X-API-Key";

    @Autowired
    ProductControllerTest(MockMvc mockMvc, ObjectMapper objectMapper) {
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
    void create_shouldReturnCreatedProduct() throws Exception {
        ProductDto dto = new ProductDto("Test", BigDecimal.valueOf(10));
        JsonApiRequest<ProductDto> req = new JsonApiRequest<>();
        req.setData(new JsonApiData<>("products", null, dto));

        Product created = new Product(1L, "Test", BigDecimal.valueOf(10), LocalDateTime.now(), null);
        when(service.create(dto.getName(), dto.getPrice())).thenReturn(created);

        mockMvc.perform(post("/products")
                        .header(API_KEY_HEADER, "valid-key")
                        .contentType("application/vnd.api+json")
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value("1"))
                .andExpect(jsonPath("$.data.attributes.name").value("Test"))
                .andExpect(jsonPath("$.data.attributes.price").value(10));
    }

    @Test
    void create_shouldReturn400_whenInvalidPrice() throws Exception {
        ProductDto dto = new ProductDto("Test", BigDecimal.ZERO);
        JsonApiRequest<ProductDto> req = new JsonApiRequest<>();
        req.setData(new JsonApiData<>("products", null, dto));

        when(service.create(dto.getName(), dto.getPrice()))
                .thenThrow(new IllegalArgumentException("price must be > 0"));

        mockMvc.perform(post("/products")
                        .header(API_KEY_HEADER, "valid-key")
                        .contentType("application/vnd.api+json")
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].status").value("400"))
                .andExpect(jsonPath("$.errors[0].title").value("Bad Request"))
                .andExpect(jsonPath("$.errors[0].detail").value("price must be > 0"));
    }

    // -------------------------
    // GET
    // -------------------------
    @Test
    void get_shouldReturnProduct_whenExists() throws Exception {
        Product product = new Product(1L, "Test", BigDecimal.valueOf(10), LocalDateTime.now(), null);
        when(service.get(1L)).thenReturn(product);

        mockMvc.perform(get("/products/1")
                        .header(API_KEY_HEADER, "valid-key"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("1"))
                .andExpect(jsonPath("$.data.attributes.name").value("Test"))
                .andExpect(jsonPath("$.data.attributes.price").value(10));
    }

    @Test
    void get_shouldReturn404_whenProductNotFound() throws Exception {
        when(service.get(1L)).thenThrow(new NoSuchElementException("Product not found."));

        mockMvc.perform(get("/products/1")
                        .header(API_KEY_HEADER, "valid-key"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errors[0].status").value("404"))
                .andExpect(jsonPath("$.errors[0].title").value("Not Found"))
                .andExpect(jsonPath("$.errors[0].detail").value("Product not found."));
    }

    // -------------------------
    // UPDATE
    // -------------------------
    @Test
    void update_shouldReturnUpdatedProduct() throws Exception {
        ProductDto dto = new ProductDto("Updated", BigDecimal.valueOf(20));
        JsonApiRequest<ProductDto> req = new JsonApiRequest<>();
        req.setData(new JsonApiData<>("products", null, dto));

        Product updated = new Product(1L, "Updated", BigDecimal.valueOf(20), LocalDateTime.now(), LocalDateTime.now());
        when(service.update(1L, dto.getName(), dto.getPrice())).thenReturn(updated);

        mockMvc.perform(put("/products/1")
                        .header(API_KEY_HEADER, "valid-key")
                        .contentType("application/vnd.api+json")
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("1"))
                .andExpect(jsonPath("$.data.attributes.name").value("Updated"))
                .andExpect(jsonPath("$.data.attributes.price").value(20));
    }

    @Test
    void update_shouldReturn404_whenProductNotFound() throws Exception {
        ProductDto dto = new ProductDto("Updated", BigDecimal.valueOf(20));
        JsonApiRequest<ProductDto> req = new JsonApiRequest<>();
        req.setData(new JsonApiData<>("products", null, dto));

        when(service.update(1L, dto.getName(), dto.getPrice()))
                .thenThrow(new NoSuchElementException("Product not found."));

        mockMvc.perform(put("/products/1")
                        .header(API_KEY_HEADER, "valid-key")
                        .contentType("application/vnd.api+json")
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errors[0].status").value("404"));
    }

    @Test
    void update_shouldReturn400_whenInvalidPrice() throws Exception {
        ProductDto dto = new ProductDto("Updated", BigDecimal.ZERO);
        JsonApiRequest<ProductDto> req = new JsonApiRequest<>();
        req.setData(new JsonApiData<>("products", null, dto));

        when(service.update(1L, dto.getName(), dto.getPrice()))
                .thenThrow(new IllegalArgumentException("Price must be > 0"));

        mockMvc.perform(put("/products/1")
                        .header(API_KEY_HEADER, "valid-key")
                        .contentType("application/vnd.api+json")
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].status").value("400"))
                .andExpect(jsonPath("$.errors[0].detail").value("Price must be > 0"));
    }

    // -------------------------
    // DELETE
    // -------------------------
    @Test
    void delete_shouldReturnNoContent() throws Exception {
        doNothing().when(service).delete(1L);

        mockMvc.perform(delete("/products/1")
                        .header(API_KEY_HEADER, "valid-key"))
                .andExpect(status().isNoContent());
    }

    @Test
    void delete_shouldReturn404_whenProductNotFound() throws Exception {
        doThrow(new NoSuchElementException("Product not found.")).when(service).delete(1L);

        mockMvc.perform(delete("/products/1")
                        .header(API_KEY_HEADER, "valid-key"))
                .andExpect(status().isNotFound());
    }

    // -------------------------
    // LIST
    // -------------------------
    @Test
    void list_shouldReturnProductList() throws Exception {
        Product p1 = new Product(1L, "A", BigDecimal.valueOf(10), LocalDateTime.now(), null);
        Product p2 = new Product(2L, "B", BigDecimal.valueOf(20), LocalDateTime.now(), null);
        when(service.list()).thenReturn(List.of(p1, p2));

        mockMvc.perform(get("/products/list")
                        .header(API_KEY_HEADER, "valid-key"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].attributes.name").value("A"))
                .andExpect(jsonPath("$.data[1].attributes.price").value(20));
    }

    // -------------------------
    // PAGINATED
    // -------------------------
    @Test
    void paginatedList_shouldReturnPage() throws Exception {
        Product p1 = new Product(1L, "A", BigDecimal.valueOf(10), LocalDateTime.now(), null);
        Product p2 = new Product(2L, "B", BigDecimal.valueOf(20), LocalDateTime.now(), null);
        Page<Product> page = new PageImpl<>(List.of(p1, p2), PageRequest.of(0, 2), 5);
        when(service.paginatedList(1, 2)).thenReturn(page);

        mockMvc.perform(get("/products/paginated?pageNumber=1&pageSize=2")
                        .header(API_KEY_HEADER, "valid-key"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.meta.totalElements").value(5))
                .andExpect(jsonPath("$.meta.totalPages").value(3))
                .andExpect(jsonPath("$.links.self").exists())
                .andExpect(jsonPath("$.links.first").exists())
                .andExpect(jsonPath("$.links.last").exists());
    }

}
