package com.linktic.inventory_service.infrastructure.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.linktic.inventory_service.config.ProductsProperties;
import com.linktic.inventory_service.domain.client.ProductsClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProductsClientRestTest {

    private RestTemplate rt;
    private ObjectMapper mapper;
    private ProductsProperties props;
    private ProductsClientRest client;

    @BeforeEach
    void setUp() {
        rt = mock(RestTemplate.class);
        mapper = new ObjectMapper();

        props = new ProductsProperties();
        props.setBaseUrl("http://products");
        ProductsProperties.ApiKey ak = new ProductsProperties.ApiKey();
        ak.setHeader("X-API-Key");
        ak.setValue("dev-products-key");
        props.setApiKey(ak);

        client = new ProductsClientRest(rt, mapper, props);
    }

    @Test
    void getProductSummary_success_parsesJsonApi_and_setsHeaders() {
        String body = """
            {"data":{"id":"123","attributes":{"name":"Phone","price":699.99}}}
            """;
        ResponseEntity<String> resp = new ResponseEntity<>(body, HttpStatus.OK);

        when(rt.exchange(
                eq("http://products/products/{id}"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(String.class),
                eq(123L)
        )).thenReturn(resp);

        ProductsClient.ProductSummary sum = client.getProductSummary(123L);

        assertEquals(123L, sum.id());
        assertEquals("Phone", sum.name());
        assertEquals(new BigDecimal("699.99"), sum.price());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<HttpEntity<Void>> captor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(rt).exchange(eq("http://products/products/{id}"), eq(HttpMethod.GET), captor.capture(), eq(String.class), eq(123L));
        HttpHeaders headers = captor.getValue().getHeaders();
        assertEquals("dev-products-key", headers.getFirst("X-API-Key"));
        assertThat(headers.getAccept()).extracting(MediaType::toString)
                .contains("application/vnd.api+json", "application/json");
    }

    @Test
    void getProductSummary_404_translatesToNoSuchElement() {
        RestClientResponseException ex = new RestClientResponseException(
                "nf", 404, "Not Found", null, null, null);

        when(rt.exchange(anyString(), any(), any(HttpEntity.class), eq(String.class), anyLong()))
                .thenThrow(ex);

        assertThrows(NoSuchElementException.class, () -> client.getProductSummary(7L));
    }

    @Test
    void getProductSummary_unexpectedResponse_wrappedAsIllegalState() {
        ResponseEntity<String> resp = new ResponseEntity<>(null, HttpStatus.OK);
        when(rt.exchange(anyString(), any(), any(HttpEntity.class), eq(String.class), anyLong()))
                .thenReturn(resp);

        IllegalStateException ise = assertThrows(IllegalStateException.class, () -> client.getProductSummary(9L));
        assertTrue(ise.getMessage().contains("Error calling Products service"));
        assertNotNull(ise.getCause());
    }

    @Test
    void getProductSummary_malformedPayload_wrappedAsIllegalState() {
        String body = """
            {"data":{"id":"55","attributes":{"name":"Item"}}}
            """;
        ResponseEntity<String> resp = new ResponseEntity<>(body, HttpStatus.OK);
        when(rt.exchange(anyString(), any(), any(HttpEntity.class), eq(String.class), anyLong()))
                .thenReturn(resp);

        IllegalStateException ise = assertThrows(IllegalStateException.class, () -> client.getProductSummary(55L));
        assertTrue(ise.getMessage().contains("Error calling Products service"));
    }

    @Test
    void existsProduct_true_whenSummaryReturnsOk() {
        String body = """
            {"data":{"id":"10","attributes":{"name":"X","price":1}}}
            """;
        when(rt.exchange(anyString(), any(), any(HttpEntity.class), eq(String.class), anyLong()))
                .thenReturn(new ResponseEntity<>(body, HttpStatus.OK));

        assertTrue(client.existsProduct(10L));
    }

    @Test
    void existsProduct_false_whenNotFound() {
        RestClientResponseException ex = new RestClientResponseException(
                "nf", 404, "Not Found", null, null, null);
        when(rt.exchange(anyString(), any(), any(HttpEntity.class), eq(String.class), anyLong()))
                .thenThrow(ex);

        assertFalse(client.existsProduct(11L));
    }

    @Test
    void existsProduct_false_whenServiceUnavailable() {
        RestClientResponseException ex = new RestClientResponseException(
                "err", 500, "Server Error", null, "oops".getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
        when(rt.exchange(anyString(), any(), any(HttpEntity.class), eq(String.class), anyLong()))
                .thenThrow(ex);

        assertFalse(client.existsProduct(12L));
    }
}
