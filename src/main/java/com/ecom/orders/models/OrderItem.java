package com.ecom.orders.models;

import com.ecom.common.models.JpaBaseModel;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.Setter;

/**
 * One line of an order. Product name and price are copied from the Product Service when the order is placed,
 * so later catalog changes do not alter past orders.
 */
@Getter
@Setter
@Entity
public class OrderItem extends JpaBaseModel {
    @JsonIgnore
    @ManyToOne
    private Order order;
    private Long productId;
    private String productName;
    /** Price per unit in paise at the time of ordering */
    private long unitPrice;
    private int quantity;
    private long lineTotal;
}
