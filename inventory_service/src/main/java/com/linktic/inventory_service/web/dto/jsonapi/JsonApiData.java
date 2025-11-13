package com.linktic.inventory_service.web.dto.jsonapi;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class JsonApiData<T> {

    private String type;
    private String id;
    private T attributes;

    public JsonApiData() {}

    public JsonApiData(String type, String id, T attributes) {
        this.type = type; this.id = id; this.attributes = attributes;
    }

}
