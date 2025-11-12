package com.linktic.products_service.web.dto.jsonapi;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class JsonApiData<T> {

    private String type;
    private String id;
    private T attributes;

    public JsonApiData() {}

    public JsonApiData(String type, String id, T attributes) {
        this.type = type; this.id = id; this.attributes = attributes;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public T getAttributes() {
        return attributes;
    }

    public void setAttributes(T attributes) {
        this.attributes = attributes;
    }

}
