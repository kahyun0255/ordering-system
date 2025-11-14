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
import lombok.ToString;

@Entity
@Table(name = "product")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@ToString
public class Product extends BaseEntity {

    @Id
    @Column(columnDefinition = "varchar(36)")
    private UUID productId;
    private String name;
    private Money price;
    private boolean available;
    private int quantity;

    public static Product create(String name, Boolean available, BigDecimal price, Integer quantity) {
        validateName(name);
        validateAvailability(available);
        validatePrice(price);
        validateQuantity(quantity);

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

    public void decreaseStock(int quantity) {
        this.quantity -= quantity;
    }

    public void increaseStock(int quantity) {
        this.quantity += quantity;
    }

    public void updateName(String name) {
        validateName(name);
        this.name = name;
    }

    public void updatePrice(BigDecimal price) {
        validatePrice(price);
        this.price = new Money(price);
    }

    public void updateAvailability(Boolean available) {
        validateAvailability(available);
        this.available = available;
    }

    public void updateQuantity(Integer quantity) {
        validateQuantity(quantity);
        this.quantity = quantity;
    }

    private static void validateName(String name) {
        if (name == null || name.length() < 2 || name.length() > 30) {
            throw new IllegalArgumentException("상품 이름은 2자 이상 30자 이하로 입력해주세요.");
        }
    }

    private static void validatePrice(BigDecimal price) {
        if (price == null || price.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("상품 가격은 0보다 커야 합니다.");
        }
    }

    private static void validateAvailability(Boolean available) {
        if (available == null) {
            throw new IllegalArgumentException("상품 판매 여부는 생략이 불가능합니다.");
        }
    }

    private static void validateQuantity(Integer quantity) {
        if (quantity == null || quantity < 0) {
            throw new IllegalArgumentException("상품 재고는 0 이상이어야 합니다.");
        }
    }
}
