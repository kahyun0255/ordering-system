package com.orderingsystem.order.domain.model;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class Restaurant {

    private UUID restaurantId;
    private final List<Product> products;
    private boolean active;

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Restaurant that = (Restaurant) o;
        return Objects.equals(restaurantId, that.restaurantId);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(restaurantId);
    }
}
