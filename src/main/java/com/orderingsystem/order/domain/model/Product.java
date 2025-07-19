package com.orderingsystem.order.domain.model;

import com.orderingsystem.common.domain.Money;
import java.util.Objects;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@AllArgsConstructor
@Getter
@Builder
public class Product {

    private UUID productId;
    private String name;
    private Money price;

    public void updateWithConfirmedNameAndPrice(String name, Money price) {
        this.name = name;
        this.price = price;
    }

    public Product(UUID productId) {
        this.productId = productId;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Product product = (Product) o;
        return Objects.equals(productId, product.productId);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(productId);
    }

}
