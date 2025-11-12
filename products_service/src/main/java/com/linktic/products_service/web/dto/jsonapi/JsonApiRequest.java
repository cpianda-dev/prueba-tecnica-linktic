package com.linktic.products_service.web.dto.jsonapi;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotNull;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class JsonApiRequest<T> {
    @NotNull
    private JsonApiData<T> data;

    public JsonApiData<T> getData() {
        return data;
    }
    public void setData(JsonApiData<T> data) {
        this.data = data;
    }
}
