package com.orderingsystem.common.saga;

public final class SagaConstants {

    private SagaConstants() {
    }

    public static final String ORDER_SAGA_NAME = "OrderProcessingSaga";

    public static final String USER_CREATED_NAME = "CreateUser";
    public static final String USER_DELETE_NAME = "DeleteUser";

    public static final String INVENTORY_COMPENSATE = "CompensateInventory";

}
