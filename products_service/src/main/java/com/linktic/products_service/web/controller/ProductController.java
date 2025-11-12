package com.linktic.products_service.web.controller;

import com.linktic.products_service.domain.model.Product;
import com.linktic.products_service.domain.service.ProductService;
import com.linktic.products_service.web.dto.jsonapi.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping(path = "/products", produces = "application/vnd.api+json")
@Validated
public class ProductController {
    private static final String TYPE = "products";
    private final ProductService service;

    public ProductController(ProductService service) {
        this.service = service;
    }

    @PostMapping(consumes = "application/vnd.api+json")
    public ResponseEntity<JsonApiResponse<ProductDto>> create(
            @Valid @RequestBody JsonApiRequest<ProductDto> req) {
        ProductDto model = req.getData().getAttributes();
        Product created = service.create(model.getName(), model.getPrice());

        JsonApiResponse<ProductDto> body = new JsonApiResponse<>();
        body.setData(new JsonApiData<>(TYPE, String.valueOf(created.getId()), ProductDto.from(created)));

        return ResponseEntity.created(URI.create("/products/" + created.getId())).body(body);
    }

    @GetMapping("/{id}")
    public ResponseEntity<JsonApiResponse<ProductDto>> get(@PathVariable Long id) {
        Product product = service.get(id);
        JsonApiResponse<ProductDto> body = new JsonApiResponse<>();
        body.setData(new JsonApiData<>(TYPE, String.valueOf(product.getId()), ProductDto.from(product)));
        return ResponseEntity.ok(body);
    }

    @PutMapping(path = "/{id}", consumes = "application/vnd.api+json")
    public ResponseEntity<JsonApiResponse<ProductDto>> update(
            @PathVariable Long id, @Valid @RequestBody JsonApiRequest<ProductDto> req) {
        ProductDto attr = req.getData().getAttributes();
        Product updated = service.update(id, attr.getName(), attr.getPrice());
        JsonApiResponse<ProductDto> body = new JsonApiResponse<>();
        body.setData(new JsonApiData<>(TYPE, String.valueOf(updated.getId()), ProductDto.from(updated)));
        return ResponseEntity.ok(body);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) { service.delete(id); }

    @GetMapping("/list")
    public ResponseEntity<JsonApiListResponse<ProductDto>> list() {
        List<Product> list = service.list();
        List<JsonApiData<ProductDto>> data = list.stream()
                .map(product -> new JsonApiData<>(TYPE,
                        String.valueOf(product.getId()),
                        ProductDto.from(product)))
                .toList();

        JsonApiListResponse<ProductDto> body = new JsonApiListResponse<>();
        body.setData(data);

        return ResponseEntity.ok(body);
    }

    @GetMapping("/paginated")
    public ResponseEntity<JsonApiListResponse<ProductDto>> paginatedList(
            @RequestParam(name = "pageNumber", defaultValue = "1") @Min(1) int pageNumber,
            @RequestParam(name = "pageSize", defaultValue = "10") @Min(1) int pageSize) {
        Page<Product> page = service.paginatedList(pageNumber, pageSize);
        List<JsonApiData<ProductDto>> data = page.getContent().stream()
                .map(product -> new JsonApiData<>(TYPE,
                        String.valueOf(product.getId()),
                        ProductDto.from(product)))
                .toList();

        JsonApiListResponse<ProductDto> body = new JsonApiListResponse<>();
        body.setData(data);

        JsonApiLinks links = new JsonApiLinks();
        links.setSelf("/products/paginated?pageNumber=" + pageNumber + "&pageSize=" + pageSize);
        links.setFirst("/products/paginated?pageNumber=1&pageSize=" + page.getSize());
        links.setLast("/products/paginated?pageNumber=" + Math.max(page.getTotalPages(),1) + "&pageSize=" + page.getSize());
        if (page.hasNext()) links.setNext("/products?pageNumber=" + (pageNumber + 1) + "&pageSize=" + page.getSize());
        if (page.hasPrevious()) links.setPrev("/products/paginated?pageNumber=" + (pageNumber - 1) + "&pageSize=" + page.getSize());
        body.setLinks(links);

        JsonApiMeta meta = new JsonApiMeta();
        meta.setTotalElements(page.getTotalElements());
        meta.setTotalPages(page.getTotalPages());
        meta.setPageNumber(pageNumber);
        meta.setPageSize(page.getSize());
        body.setMeta(meta);

        return ResponseEntity.ok(body);
    }

}
