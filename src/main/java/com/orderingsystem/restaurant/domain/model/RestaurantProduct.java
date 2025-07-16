package com.orderingsystem.restaurant.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "restaurant_restaurant_products")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RestaurantProduct {

    @Id
    @Column(columnDefinition = "varchar(36)")
    private UUID id;
    @Column(columnDefinition = "varchar(36)")
    private UUID restaurantId;
    @Column(columnDefinition = "varchar(36)")
    private UUID productId;

}
