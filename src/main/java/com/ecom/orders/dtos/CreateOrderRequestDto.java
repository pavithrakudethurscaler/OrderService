package com.ecom.orders.dtos;

import com.ecom.orders.models.DeliveryAddress;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CreateOrderRequestDto {
    private List<OrderItemRequestDto> items;
    private DeliveryAddress deliveryAddress;
}
