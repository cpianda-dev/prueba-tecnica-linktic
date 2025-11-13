package com.linktic.inventory_service.web.dto.jsonapi;

import lombok.Getter;

import java.util.List;

@Getter
public class JsonApiErrorResponse {
    private List<JsonApiError> errors;
    public JsonApiErrorResponse(List<JsonApiError> errors){
        this.errors = errors;
    }
}
