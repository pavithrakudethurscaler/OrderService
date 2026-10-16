package com.ecom.orders.models;

import java.util.Map;
import java.util.Set;

public enum OrderStatus {
    /** Placed, waiting for payment */
    CREATED,
    PAID,
    SHIPPED,
    DELIVERED,
    CANCELLED;

    private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED_TRANSITIONS = Map.of(
            CREATED, Set.of(PAID, CANCELLED),
            PAID, Set.of(SHIPPED),
            SHIPPED, Set.of(DELIVERED),
            DELIVERED, Set.of(),
            CANCELLED, Set.of());

    public boolean canMoveTo(OrderStatus next) {
        return ALLOWED_TRANSITIONS.get(this).contains(next);
    }
}
