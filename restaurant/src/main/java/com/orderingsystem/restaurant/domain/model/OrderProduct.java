package com.orderingsystem.restaurant.domain.model;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class OrderProduct {

    private Product product;
    private int quantity;

}
