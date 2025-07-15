package com.orderingsystem.order.domain.model;

import java.util.Objects;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class Address {
    private final UUID id;
    private final String street;
    private final String postalCode;
    private final String city;

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Address that = (Address) o;
        return Objects.equals(street, that.street) && Objects.equals(postalCode, that.postalCode)
                && Objects.equals(city, that.city);
    }

    @Override
    public int hashCode() {
        return Objects.hash(street, postalCode, city);
    }
}
