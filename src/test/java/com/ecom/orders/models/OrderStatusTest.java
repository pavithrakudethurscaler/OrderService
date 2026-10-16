package com.ecom.orders.models;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class OrderStatusTest {
    @ParameterizedTest(name = "{0} -> {1} allowed={2}")
    @CsvSource({
            "CREATED,PAID,true", "CREATED,CANCELLED,true", "CREATED,SHIPPED,false", "CREATED,DELIVERED,false", "CREATED,CREATED,false",
            "PAID,SHIPPED,true", "PAID,CANCELLED,false", "PAID,DELIVERED,false", "PAID,PAID,false", "PAID,CREATED,false",
            "SHIPPED,DELIVERED,true", "SHIPPED,CANCELLED,false", "SHIPPED,PAID,false", "SHIPPED,SHIPPED,false", "SHIPPED,CREATED,false",
            "DELIVERED,CREATED,false", "DELIVERED,PAID,false", "DELIVERED,SHIPPED,false", "DELIVERED,CANCELLED,false", "DELIVERED,DELIVERED,false",
            "CANCELLED,CREATED,false", "CANCELLED,PAID,false", "CANCELLED,SHIPPED,false", "CANCELLED,DELIVERED,false", "CANCELLED,CANCELLED,false"
    })
    void transitionMatrix(OrderStatus from, OrderStatus to, boolean allowed) {
        assertThat(from.canMoveTo(to)).isEqualTo(allowed);
    }
}
