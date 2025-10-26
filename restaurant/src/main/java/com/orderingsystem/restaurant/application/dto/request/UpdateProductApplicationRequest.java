package com.orderingsystem.restaurant.application.dto.request;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder
@ToString
public class UpdateProductApplicationRequest {

    private String name;
    private BigDecimal price;
    private Boolean available;
    private Integer quantity;

}
