package com.orderingsystem.common.domain.status;

import lombok.Getter;

@Getter
public enum DebeziumOp {
    CREATE("c"), UPDATE("u"), DELETE("d");

    private final String value;

    DebeziumOp(String value) {
        this.value = value;
    }
}
