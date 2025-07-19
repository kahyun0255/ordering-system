package com.orderingsystem.restaurant.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "restaurant_restaurants")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Restaurant {

    @Id
    @Column(columnDefinition = "varchar(36)")
    private UUID restaurantId;
    private String name;
    private Boolean active;

    @Transient
    private OrderDetail orderDetail;

}
