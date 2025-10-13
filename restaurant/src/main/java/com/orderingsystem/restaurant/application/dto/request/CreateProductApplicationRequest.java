package com.orderingsystem.restaurant.application.dto.request;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CreateProductApplicationRequest {

    private String name;
    private BigDecimal price;
    private Boolean available;
    private Integer quantity;

}
