package com.orderingsystem.restaurant.domain.model;

import com.orderingsystem.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "restaurants")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Restaurant extends BaseEntity {

    @Id
    @Column(columnDefinition = "varchar(36)")
    private UUID restaurantId;
    private String name;
    private Boolean active;

    @Transient
    private OrderDetail orderDetail;

}
