package com.linktic.products_service.web.handler;

import com.linktic.products_service.web.controller.ProductController;
import com.linktic.products_service.web.dto.jsonapi.JsonApiErrorResponse;
import com.linktic.products_service.web.dto.jsonapi.JsonApiRequest;
import com.linktic.products_service.web.dto.jsonapi.ProductDto;
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
        ProductDto target = new ProductDto();
        BeanPropertyBindingResult br = new BeanPropertyBindingResult(target, "product");
        br.addError(new FieldError("product", "name", "must not be blank"));

        Method m = ProductController.class.getMethod("create", JsonApiRequest.class);
        MethodParameter mp = new MethodParameter(m, 0);

        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(mp, br);

        ResponseEntity<?> resp = handler.handleMethodArgumentNotValid(
                ex, new HttpHeaders(), HttpStatus.BAD_REQUEST, new ServletWebRequest(new MockHttpServletRequest()));

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getHeaders().getFirst("Content-Type")).isEqualTo("application/vnd.api+json");

        JsonApiErrorResponse body = (JsonApiErrorResponse) resp.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getErrors()).isNotEmpty();
        assertThat(body.getErrors().get(0).getStatus()).isEqualTo("400");
        assertThat(body.getErrors().get(0).getTitle()).isEqualTo("Validation Error");
        assertThat(body.getErrors().get(0).getDetail()).contains("name").contains("must not be blank");
    }
}
