package com.linktic.inventory_service.web.controller;

import com.linktic.inventory_service.domain.model.Inventory;
import com.linktic.inventory_service.domain.model.InventoryDetails;
import com.linktic.inventory_service.domain.service.InventoryService;
import com.linktic.inventory_service.web.dto.InventoryDto;
import com.linktic.inventory_service.web.dto.PurchaseDto;
import com.linktic.inventory_service.web.dto.jsonapi.JsonApiData;
import com.linktic.inventory_service.web.dto.jsonapi.JsonApiLinks;
import com.linktic.inventory_service.web.dto.jsonapi.JsonApiListResponse;
import com.linktic.inventory_service.web.dto.jsonapi.JsonApiMeta;
import com.linktic.inventory_service.web.dto.jsonapi.JsonApiRequest;
import com.linktic.inventory_service.web.dto.jsonapi.JsonApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(path = "/inventories", produces = "application/vnd.api+json")
@Validated
public class InventoryController {
    private static final String TYPE = "inventories";
    private final InventoryService service;

    public InventoryController(InventoryService service) {
        this.service = service;
    }

    @PostMapping(consumes = "application/vnd.api+json")
    public ResponseEntity<JsonApiResponse<InventoryDto>> create(
            @Valid @RequestBody JsonApiRequest<InventoryDto> req) {
        InventoryDto model = req.getData().getAttributes();
        Inventory created = service.create(model.getProductId(), model.getQuantity());

        JsonApiResponse<InventoryDto> body = new JsonApiResponse<>();
        body.setData(new JsonApiData<>(TYPE, String.valueOf(created.getId()), InventoryDto.from(created)));

        return ResponseEntity.created(URI.create("/inventories/" + created.getId())).body(body);
    }

    @GetMapping("/{id}")
    public ResponseEntity<JsonApiResponse<InventoryDto>> get(@PathVariable Long id) {
        Inventory inventory = service.get(id);
        JsonApiResponse<InventoryDto> body = new JsonApiResponse<>();
        body.setData(new JsonApiData<>(TYPE, String.valueOf(inventory.getId()), InventoryDto.from(inventory)));
        return ResponseEntity.ok(body);
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<JsonApiResponse<Map<String, Object>>> getByProductId(@PathVariable Long productId) {
        InventoryDetails details = service.getDetailsByProductId(productId);

        Map<String, Object> attrs = new HashMap<>();
        attrs.put("productId", details.inventory().getProductId());
        attrs.put("quantity", details.inventory().getQuantity());

        Map<String, Object> product = new HashMap<>();
        product.put("id", details.product().id());
        product.put("name", details.product().name());
        product.put("price", details.product().price());
        attrs.put("product", product);

        JsonApiResponse<Map<String, Object>> body = new JsonApiResponse<>();
        body.setData(new JsonApiData<>("inventories",
                String.valueOf(details.inventory().getId()), attrs));

        JsonApiLinks links = new JsonApiLinks();
        links.setSelf("/inventories/product/" + productId);
        body.setLinks(links);

        return ResponseEntity.ok(body);
    }

    @PutMapping(path = "/{id}", consumes = "application/vnd.api+json")
    public ResponseEntity<JsonApiResponse<InventoryDto>> update(
            @PathVariable Long id, @Valid @RequestBody JsonApiRequest<InventoryDto> req) {
        InventoryDto model = req.getData().getAttributes();
        Inventory updated = service.update(id, model.getQuantity());
        JsonApiResponse<InventoryDto> body = new JsonApiResponse<>();
        body.setData(new JsonApiData<>(TYPE, String.valueOf(updated.getId()), InventoryDto.from(updated)));
        return ResponseEntity.ok(body);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) { service.delete(id); }

    @GetMapping("/list")
    public ResponseEntity<JsonApiListResponse<InventoryDto>> list() {
        List<Inventory> list = service.list();
        List<JsonApiData<InventoryDto>> data = list.stream()
                .map(product -> new JsonApiData<>(TYPE,
                        String.valueOf(product.getId()),
                        InventoryDto.from(product)))
                .toList();

        JsonApiListResponse<InventoryDto> body = new JsonApiListResponse<>();
        body.setData(data);

        return ResponseEntity.ok(body);
    }

    @GetMapping("/paginated")
    public ResponseEntity<JsonApiListResponse<InventoryDto>> paginatedList(
            @RequestParam(name = "pageNumber", defaultValue = "1") @Min(1) int pageNumber,
            @RequestParam(name = "pageSize", defaultValue = "10") @Min(1) int pageSize) {
        Page<Inventory> page = service.paginatedList(pageNumber, pageSize);
        List<JsonApiData<InventoryDto>> data = page.getContent().stream()
                .map(product -> new JsonApiData<>(TYPE,
                        String.valueOf(product.getId()),
                        InventoryDto.from(product)))
                .toList();

        JsonApiListResponse<InventoryDto> body = new JsonApiListResponse<>();
        body.setData(data);

        JsonApiLinks links = new JsonApiLinks();
        links.setSelf("/inventories/paginated?pageNumber=" + pageNumber + "&pageSize=" + pageSize);
        links.setFirst("/inventories/paginated?pageNumber=1&pageSize=" + page.getSize());
        links.setLast("/inventories/paginated?pageNumber=" + Math.max(page.getTotalPages(),1) + "&pageSize=" + page.getSize());
        if (page.hasNext()) links.setNext("/inventories/paginated?pageNumber=" + (pageNumber + 1) + "&pageSize=" + page.getSize());
        if (page.hasPrevious()) links.setPrev("/inventories/paginated?pageNumber=" + (pageNumber - 1) + "&pageSize=" + page.getSize());
        body.setLinks(links);

        JsonApiMeta meta = new JsonApiMeta();
        meta.setTotalElements(page.getTotalElements());
        meta.setTotalPages(page.getTotalPages());
        meta.setPageNumber(pageNumber);
        meta.setPageSize(page.getSize());
        body.setMeta(meta);

        return ResponseEntity.ok(body);
    }

    @PostMapping(path = "/purchase", consumes = "application/vnd.api+json")
    public ResponseEntity<JsonApiResponse<InventoryDto>> purchase(
            @Valid @RequestBody JsonApiRequest<PurchaseDto> req) {

        PurchaseDto dto = req.getData().getAttributes();
        var updated = service.purchase(dto.getProductId(), dto.getUnits());

        JsonApiResponse<InventoryDto> body = new JsonApiResponse<>();
        body.setData(new JsonApiData<>("inventories", String.valueOf(updated.getId()), InventoryDto.from(updated)));

        JsonApiLinks links = new JsonApiLinks();
        links.setSelf("/inventories/product/" + dto.getProductId());
        body.setLinks(links);

        return ResponseEntity.ok(body);
    }

}
