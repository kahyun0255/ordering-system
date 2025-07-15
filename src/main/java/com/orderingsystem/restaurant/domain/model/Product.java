package com.orderingsystem.restaurant.domain.model;

import com.orderingsystem.common.domain.Money;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class Product {

    private final UUID productId;
    private String name;
    private Money price;
    private final int quantity;
    private boolean available;

    public void updateWithConfirmedNamePriceAndAvailability(String name, Money price, boolean available) {
        this.name = name;
        this.price = price;
        this.available = available;
    }

}
