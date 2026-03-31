package com.orderingsystem.restaurant.domain.model;

import com.orderingsystem.common.domain.AggregateRoot;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
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
public class Restaurant extends AggregateRoot {

    @Id
    @Column(columnDefinition = "varchar(36)")
    private UUID restaurantId;
    private String name;

    @Enumerated(EnumType.STRING)
    private RestaurantStatus status;

    public static Restaurant create(UUID restaurantId, String name) {
        return Restaurant.builder()
                .restaurantId(restaurantId)
                .name(normalizeAndValidateName(name))
                .status(RestaurantStatus.PENDING_APPROVAL)
                .build();
    }

    public void updateName(String name) {
        this.name = normalizeAndValidateName(name);
    }

    private static String normalizeAndValidateName(String name) {
        if (name == null) {
            throw new IllegalArgumentException("레스토랑 이름은 null일 수 없습니다.");
        }
        String trimmed = name.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("레스토랑 이름은 비어있을 수 없습니다.");
        }
        return trimmed;
    }

    public void delete() {
        this.status = RestaurantStatus.DELETED;
    }

    public void updateStatusByOwner(RestaurantStatus status) {
        if (!this.status.ownerCanChangeTo(status)){
            throw new IllegalArgumentException("해당 상태로 변경이 불가능합니다.");
        }

        this.status = status;
    }

    public void updateStatusByAdmin(RestaurantStatus status) {
        this.status = status;
    }
}
