package com.orderingsystem.common.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Embeddable
@NoArgsConstructor
@AllArgsConstructor
public class Money {

    @Column(name = "price")
    private BigDecimal amount;

    public static final Money ZERO = new Money(BigDecimal.ZERO);

    public boolean isGreaterThanZero() {
        return this.amount != null && this.amount.compareTo(BigDecimal.ZERO) > 0;
    }

    public boolean isGreaterThan(Money money) {
        return this.amount != null && this.amount.compareTo(money.getAmount()) > 0;
    }

    public Money add(Money money) {
        return new Money(setScale(this.amount.add(money.getAmount())));
    }

    public Money subtract(Money money) {
        return new Money(setScale(this.amount.subtract(money.getAmount())));
    }

    public Money multiply(int multiplier) {
        return new Money(setScale(this.amount.multiply(new BigDecimal(multiplier))));
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public boolean equalTo(Money other) {
        if (this.amount == null || other == null || other.amount == null) {
            return false;
        }
        return this.amount.compareTo(other.amount) == 0;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Money m)) {
            return false;
        }
        if (this.amount == null || m.amount == null) {
            return this.amount == m.amount;
        }
        return this.amount.compareTo(m.amount) == 0;
    }

    @Override
    public int hashCode() {
        return amount == null ? 0 : amount.stripTrailingZeros().hashCode();
    }

    private BigDecimal setScale(BigDecimal input) {
        return input.setScale(2, RoundingMode.HALF_EVEN);
    }
}
