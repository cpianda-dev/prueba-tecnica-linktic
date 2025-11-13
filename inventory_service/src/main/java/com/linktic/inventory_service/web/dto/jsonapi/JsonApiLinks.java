package com.linktic.inventory_service.web.dto.jsonapi;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class JsonApiLinks {
    private String self;
    private String first;
    private String last;
    private String next;
    private String prev;

}

