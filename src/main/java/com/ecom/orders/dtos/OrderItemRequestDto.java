package com.ecom.orders.dtos;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderItemRequestDto {
    private Long productId;
    private Integer quantity;
}
