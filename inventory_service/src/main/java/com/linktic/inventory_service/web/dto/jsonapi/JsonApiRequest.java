package com.linktic.inventory_service.web.dto.jsonapi;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class JsonApiRequest<T> {
    @NotNull
    private JsonApiData<T> data;

}
