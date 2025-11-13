package com.linktic.inventory_service.web.dto.jsonapi;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class JsonApiError {

    private String status;
    private String title;
    private String detail;

    public JsonApiError() {}

    public JsonApiError(String status, String title, String detail) {
        this.status = status;
        this.title = title;
        this.detail = detail;
    }

}
