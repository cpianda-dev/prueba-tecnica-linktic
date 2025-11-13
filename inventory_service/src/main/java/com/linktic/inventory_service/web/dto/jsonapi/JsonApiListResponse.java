package com.linktic.inventory_service.web.dto.jsonapi;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class JsonApiListResponse<T> {

    private List<JsonApiData<T>> data;
    private JsonApiLinks links;
    private JsonApiMeta meta;

}
