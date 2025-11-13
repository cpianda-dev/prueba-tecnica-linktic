package com.linktic.inventory_service.infrastructure.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.linktic.inventory_service.config.ProductsProperties;
import com.linktic.inventory_service.domain.client.ProductsClient;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.NoSuchElementException;

@Component
public class ProductsClientRest implements ProductsClient {

    private final RestTemplate rt;
    private final ObjectMapper mapper;
    private final ProductsProperties props;

    public ProductsClientRest(RestTemplate productsRestTemplate, ObjectMapper mapper, ProductsProperties props) {
        this.rt = productsRestTemplate;
        this.mapper = mapper;
        this.props = props;
    }

    @Override
    public boolean existsProduct(Long productId) {
        try {
            getProductSummary(productId);
            return true;
        } catch (NoSuchElementException notFound) {
            return false;
        } catch (RuntimeException ex) {
            // indisponible o error inesperado: considera falso
            return false;
        }
    }

    @Override
    public ProductsClient.ProductSummary getProductSummary(Long productId) {
        try {
            HttpHeaders h = new HttpHeaders();
            h.setAccept(MediaType.parseMediaTypes("application/vnd.api+json, application/json"));
            h.set(props.getApiKey().getHeader(), props.getApiKey().getValue());
            HttpEntity<Void> entity = new HttpEntity<>(h);

            ResponseEntity<String> resp = rt.exchange(
                    props.getBaseUrl() + "/products/{id}",
                    HttpMethod.GET,
                    entity,
                    String.class,
                    productId
            );

            if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null) {
                throw new IllegalStateException("Products service unexpected response");
            }

            JsonNode root = mapper.readTree(resp.getBody());
            JsonNode data = root.path("data");
            if (data.isMissingNode() || data.isNull()) {
                throw new IllegalStateException("Products service malformed JSON:API");
            }

            Long id = parseLongSafe(data.path("id").asText(null));
            JsonNode attrs = data.path("attributes");
            String name = attrs.path("name").asText(null);
            BigDecimal price = attrs.hasNonNull("price")
                    ? attrs.path("price").decimalValue()
                    : null;

            if (id == null || name == null || price == null) {
                throw new IllegalStateException("Products service incomplete product payload");
            }

            return new ProductsClient.ProductSummary(id, name, price);
        } catch (RestClientResponseException ex) {
            if (HttpStatus.NOT_FOUND.equals(ex.getStatusCode())) {
                throw new NoSuchElementException("Product not found.");
            }
            throw new IllegalStateException("Products service unavailable");
        } catch (Exception ex) {
            throw new IllegalStateException("Error calling Products service", ex);
        }
    }

    private static Long parseLongSafe(String s) {
        if (s == null) return null;
        try { return Long.valueOf(s); } catch (NumberFormatException e) { return null; }
    }
}
