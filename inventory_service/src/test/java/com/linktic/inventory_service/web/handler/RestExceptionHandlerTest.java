package com.linktic.inventory_service.web.handler;

import com.linktic.inventory_service.web.controller.InventoryController;
import com.linktic.inventory_service.web.dto.jsonapi.JsonApiErrorResponse;
import com.linktic.inventory_service.web.dto.jsonapi.JsonApiRequest;
import com.linktic.inventory_service.web.dto.InventoryDto;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.ServletWebRequest;

import java.lang.reflect.Method;
import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.assertThat;

class RestExceptionHandlerTest {

    private final RestExceptionHandler handler = new RestExceptionHandler();

    @Test
    void handleNotFound_returns404_withJsonApiEnvelope() {
        ResponseEntity<?> resp = handler.handleNotFound(new NoSuchElementException("missing"));
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(resp.getHeaders().getFirst("Content-Type")).isEqualTo("application/vnd.api+json");

        JsonApiErrorResponse body = (JsonApiErrorResponse) resp.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getErrors()).isNotEmpty();
        assertThat(body.getErrors().get(0).getStatus()).isEqualTo("404");
        assertThat(body.getErrors().get(0).getTitle()).isEqualTo("Not Found");
        assertThat(body.getErrors().get(0).getDetail()).isEqualTo("missing");
    }

    @Test
    void handleBadRequest_returns400_withJsonApiEnvelope() {
        ResponseEntity<?> resp = handler.handleBadRequest(new IllegalArgumentException("bad"));
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getHeaders().getFirst("Content-Type")).isEqualTo("application/vnd.api+json");

        JsonApiErrorResponse body = (JsonApiErrorResponse) resp.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getErrors().get(0).getStatus()).isEqualTo("400");
        assertThat(body.getErrors().get(0).getTitle()).isEqualTo("Bad Request");
        assertThat(body.getErrors().get(0).getDetail()).isEqualTo("bad");
    }

    @Test
    void handleMethodArgumentNotValid_returns400WithFirstFieldMessage() throws NoSuchMethodException {
        // Simula un error de validación en InventoryDto.quantity
        InventoryDto target = new InventoryDto();
        BeanPropertyBindingResult br = new BeanPropertyBindingResult(target, "inventory");
        br.addError(new FieldError("inventory", "quantity", "must be >= 0"));

        Method m = InventoryController.class.getMethod("create", JsonApiRequest.class);
        MethodParameter mp = new MethodParameter(m, 0);

        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(mp, br);

        ResponseEntity<?> resp = handler.handleMethodArgumentNotValid(
                ex, new HttpHeaders(), HttpStatus.BAD_REQUEST, new ServletWebRequest(new MockHttpServletRequest()));

        Assertions.assertNotNull(resp);
        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getHeaders().getFirst("Content-Type")).isEqualTo("application/vnd.api+json");

        JsonApiErrorResponse body = (JsonApiErrorResponse) resp.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getErrors()).isNotEmpty();
        assertThat(body.getErrors().get(0).getStatus()).isEqualTo("400");
        assertThat(body.getErrors().get(0).getTitle()).isEqualTo("Validation Error");
        assertThat(body.getErrors().get(0).getDetail()).contains("quantity").contains("must be >= 0");
    }
}
