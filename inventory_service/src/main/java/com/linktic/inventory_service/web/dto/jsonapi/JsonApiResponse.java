package com.linktic.inventory_service.web.dto.jsonapi;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class JsonApiResponse<T> {

    private JsonApiData<T> data;
    private JsonApiLinks links;

}
