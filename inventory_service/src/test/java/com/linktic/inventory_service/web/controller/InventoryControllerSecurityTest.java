package com.linktic.inventory_service.web.controller;

import com.linktic.inventory_service.config.SecurityConfig;
import com.linktic.inventory_service.domain.service.InventoryService;
import com.linktic.inventory_service.web.handler.RestExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = InventoryController.class)
@Import({SecurityConfig.class, RestExceptionHandler.class})
@TestPropertySource(properties = {
        "security.api-key.header=X-API-Key",
        "security.api-key.value=valid-key"
})

class InventoryControllerSecurityTest {

    private final MockMvc mockMvc;

    @MockitoBean
    private InventoryService service;

    private static final String API_KEY_HEADER = "X-API-Key";

    @Autowired
    InventoryControllerSecurityTest(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }

    @Test
    void anyEndpoint_shouldReturn401_whenNoApiKey() throws Exception {
        mockMvc.perform(get("/inventories/1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void anyEndpoint_shouldReturn401_whenWrongApiKey() throws Exception {
        mockMvc.perform(get("/inventories/1")
                        .header(API_KEY_HEADER, "wrong"))
                .andExpect(status().isUnauthorized());
    }
}
