package com.linktic.products_service.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.linktic.products_service.infrastructure.mapper.ProductMapperImpl;
import com.linktic.products_service.infrastructure.persistence.adapter.ProductRepositoryAdapter;
import com.linktic.products_service.infrastructure.persistence.entity.ProductEntity;
import com.linktic.products_service.infrastructure.persistence.jpa.ProductJpaRepository;
import com.linktic.products_service.web.dto.jsonapi.JsonApiData;
import com.linktic.products_service.web.dto.jsonapi.JsonApiRequest;
import com.linktic.products_service.web.dto.jsonapi.ProductDto;
import com.linktic.products_service.web.handler.RestExceptionHandler;
import com.linktic.products_service.domain.service.ProductService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatcher;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ProductController.class)
@AutoConfigureMockMvc(addFilters = false)
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
@Import({ RestExceptionHandler.class, ProductService.class, ProductRepositoryAdapter.class, ProductMapperImpl.class })
class ProductInternalIT {

    private static final MediaType JSON_API = MediaType.valueOf("application/vnd.api+json");

    @MockitoBean
    ProductJpaRepository jpa; // único mock

    private final MockMvc mvc;
    private final ObjectMapper om;

    ProductInternalIT(MockMvc mvc, ObjectMapper om) {
        this.mvc = mvc;
        this.om = om;
    }

    @BeforeEach
    void resetMocks() { reset(jpa); }

    @Test
    void create_creaProducto_viaPipeline_retorna201_JSONAPI() throws Exception {
        ProductDto dto = new ProductDto("Mouse", BigDecimal.valueOf(19.99));
        JsonApiRequest<ProductDto> req = new JsonApiRequest<>();
        req.setData(new JsonApiData<>("products", null, dto));

        when(jpa.save(argThat(entityMatches(null, "Mouse", BigDecimal.valueOf(19.99)))))
                .thenAnswer(inv -> {
                    ProductEntity e = inv.getArgument(0);
                    e.setId(1L);
                    e.setCreatedAt(LocalDateTime.now());
                    return e;
                });

        mvc.perform(post("/products")
                        .contentType(JSON_API).accept(JSON_API)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/products/1"))
                .andExpect(jsonPath("$.data.type").value("products"))
                .andExpect(jsonPath("$.data.id").value("1"))
                .andExpect(jsonPath("$.data.attributes.name").value("Mouse"))
                .andExpect(jsonPath("$.data.attributes.price").value(19.99));

        verify(jpa, times(1)).save(any(ProductEntity.class));
    }

    @Test
    void get_devuelveProducto_existente() throws Exception {
        ProductEntity e = entity(7L, "Keyboard", BigDecimal.valueOf(49.5));
        when(jpa.findById(7L)).thenReturn(Optional.of(e));

        mvc.perform(get("/products/7").accept(JSON_API))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("7"))
                .andExpect(jsonPath("$.data.attributes.name").value("Keyboard"))
                .andExpect(jsonPath("$.data.attributes.price").value(49.5));
    }

    @Test
    void get_retorna404_cuandoNoExiste() throws Exception {
        when(jpa.findById(99L)).thenReturn(Optional.empty());

        mvc.perform(get("/products/99").accept(JSON_API))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errors[0].title").value("Not Found"))
                .andExpect(jsonPath("$.errors[0].detail").value("Product not found."));
    }

    @Test
    void update_actualizaNombreYPrecio() throws Exception {
        ProductDto dto = new ProductDto("Nuevo", BigDecimal.valueOf(25));
        JsonApiRequest<ProductDto> req = new JsonApiRequest<>();
        req.setData(new JsonApiData<>("products", null, dto));

        ProductEntity current = entity(3L, "Viejo", BigDecimal.valueOf(10));
        when(jpa.findById(3L)).thenReturn(Optional.of(current));
        when(jpa.save(argThat(entityMatches(3L, "Nuevo", BigDecimal.valueOf(25)))))
                .thenAnswer(inv -> {
                    ProductEntity e = inv.getArgument(0);
                    e.setUpdatedAt(LocalDateTime.now());
                    return e;
                });

        mvc.perform(put("/products/3")
                        .contentType(JSON_API).accept(JSON_API)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("3"))
                .andExpect(jsonPath("$.data.attributes.name").value("Nuevo"))
                .andExpect(jsonPath("$.data.attributes.price").value(25.0));

        verify(jpa).findById(3L);
        verify(jpa).save(any(ProductEntity.class));
    }

    @Test
    void delete_elimina_yRetorna204() throws Exception {
        doNothing().when(jpa).deleteById(5L);

        mvc.perform(delete("/products/5").accept(JSON_API))
                .andExpect(status().isNoContent());

        verify(jpa).deleteById(5L);
    }

    @Test
    void list_retornaListadoMapeado() throws Exception {
        when(jpa.findAll()).thenReturn(List.of(
                entity(1L, "A", BigDecimal.TEN),
                entity(2L, "B", BigDecimal.valueOf(20))
        ));

        mvc.perform(get("/products/list").accept(JSON_API))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].attributes.name").value("A"))
                .andExpect(jsonPath("$.data[1].attributes.price").value(20));
    }

    @Test
    void paginated_retornaPagina_conLinksYMeta() throws Exception {
        PageImpl<ProductEntity> page = new PageImpl<>(
                List.of(
                        entity(1L, "A", BigDecimal.TEN),
                        entity(2L, "B", BigDecimal.valueOf(20))
                ),
                PageRequest.of(0, 2, Sort.unsorted()),
                5
        );
        when(jpa.findAll(any(Pageable.class))).thenReturn(page);

        mvc.perform(get("/products/paginated?pageNumber=1&pageSize=2").accept(JSON_API))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.meta.totalElements").value(5))
                .andExpect(jsonPath("$.meta.totalPages").value(3))
                .andExpect(jsonPath("$.links.self").exists())
                .andExpect(jsonPath("$.links.first").exists())
                .andExpect(jsonPath("$.links.last").exists());
    }

    private static ProductEntity entity(Long id, String name, BigDecimal price) {
        ProductEntity e = new ProductEntity();
        e.setId(id);
        e.setName(name);
        e.setPrice(price);
        e.setCreatedAt(LocalDateTime.now());
        return e;
    }

    private static ArgumentMatcher<ProductEntity> entityMatches(Long id, String name, BigDecimal price) {
        return e -> e != null
                && (id == null || id.equals(e.getId()))
                && (name == null || name.equals(e.getName()))
                && (price == null || price.compareTo(e.getPrice()) == 0);
    }
}
