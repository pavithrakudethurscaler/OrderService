package com.ecom.orders.dtos;

import com.ecom.orders.models.OrderStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateOrderStatusRequestDto {
    private OrderStatus status;
    /** Optional, e.g. a courier tracking number */
    private String note;
}
