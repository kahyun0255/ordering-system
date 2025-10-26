package com.orderingsystem.common.saga;

public final class SagaConstants {

    private SagaConstants() {
    }

    public static final String ORDER_SAGA_NAME = "OrderProcessingSaga";

    public static final String USER_CREATED_NAME = "CreateUser";
    public static final String USER_DELETE_NAME = "DeleteUser";

    public static final String STOCK_RESERVED_NAME = "StockReserved";
    public static final String STOCK_CONFIRMED_NAME = "StockConfirmed";
    public static final String STOCK_CANCELLED_NAME = "StockCancelled";
    public static final String STOCK_RESERVE_CANCELLED_NAME = "StockReserveCancelled";

}
