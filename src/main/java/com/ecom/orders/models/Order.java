package com.ecom.orders.models;

import com.ecom.common.models.JpaBaseModel;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "orders") // "order" is a reserved word in SQL
public class Order extends JpaBaseModel {
    /** Id of the customer in the User Service */
    private Long userId;
    @Enumerated(EnumType.STRING)
    private OrderStatus status;
    /** Sum of all line totals, in paise */
    private long totalAmount;
    private String currency;
    @Embedded
    private DeliveryAddress deliveryAddress;
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("changedAt ASC, id ASC")
    private List<OrderStatusChange> statusHistory = new ArrayList<>();
    /** Payment Service id of the payment that paid this order */
    private String paymentId;
    private long createdAt;
    private long updatedAt;
    /** Optimistic lock: two concurrent status changes (e.g. two payments) cannot both succeed */
    @Version
    private Long version;
}
