package com.linktic.inventory_service.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

class ApiKeyAuthFilterTest {

    private final ApiKeyAuthFilter filter = new ApiKeyAuthFilter("X-API-Key", "valid-key");

    @Test
    void shouldNotFilter_forSwaggerAndActuator() {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/v3/api-docs");
        assertThat(filter.shouldNotFilter(req)).isTrue();

        req = new MockHttpServletRequest("GET", "/actuator/health");
        assertThat(filter.shouldNotFilter(req)).isTrue();
    }

    @Test
    void doFilter_allows_whenHeaderMatches() throws ServletException, IOException {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/inventories/1");
        req.addHeader("X-API-Key", "valid-key");
        MockHttpServletResponse res = new MockHttpServletResponse();

        FilterChain chain = (request, response) -> ((MockHttpServletResponse) response).setStatus(200);

        filter.doFilter(req, res, chain);

        assertThat(res.getStatus()).isEqualTo(200);
    }

    @Test
    void doFilter_denies_whenHeaderWrong() throws ServletException, IOException {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/inventories/1");
        req.addHeader("X-API-Key", "wrong");
        MockHttpServletResponse res = new MockHttpServletResponse();

        FilterChain chain = (request, response) -> fail("Chain must NOT be invoked on 401");

        filter.doFilter(req, res, chain);

        assertThat(res.getStatus()).isEqualTo(401);
        assertThat(res.getContentAsString()).contains("Invalid API Key");
        assertThat(res.getContentType()).isEqualTo("application/vnd.api+json");
    }
}
