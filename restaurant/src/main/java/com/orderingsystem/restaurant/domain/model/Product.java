package com.orderingsystem.restaurant.domain.model;

import com.orderingsystem.common.domain.BaseEntity;
import com.orderingsystem.common.domain.Money;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "product")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Product extends BaseEntity {

    @Id
    @Column(columnDefinition = "varchar(36)")
    private UUID productId;
    private String name;
    private Money price;
    private boolean available;
    private int quantity;

    public static Product create(String name, Boolean available, BigDecimal price, Integer quantity) {
        return Product.builder()
                .productId(UUID.randomUUID())
                .name(name)
                .available(available)
                .price(new Money(price))
                .quantity(quantity)
                .build();
    }

    public void updateWithConfirmedNamePriceAndAvailability(String name, Money price, boolean available) {
        this.name = name;
        this.price = price;
        this.available = available;
    }

    public void delete() {
        this.available = false;
    }
}
