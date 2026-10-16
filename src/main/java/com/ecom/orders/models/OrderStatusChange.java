package com.ecom.orders.models;

import com.ecom.common.models.JpaBaseModel;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.Setter;

/** Tracking history: one row per status the order has been in (PRD 4.3). */
@Getter
@Setter
@Entity
public class OrderStatusChange extends JpaBaseModel {
    @ManyToOne
    private Order order;
    @Enumerated(EnumType.STRING)
    private OrderStatus status;
    private long changedAt;
    private String note;
}
