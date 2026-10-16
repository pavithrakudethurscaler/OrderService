package com.ecom.orders.dtos;

import com.ecom.orders.models.DeliveryAddress;
import com.ecom.orders.models.Order;
import com.ecom.orders.models.OrderStatus;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class OrderResponseDto {
    private Long id;
    private OrderStatus status;
    private long totalAmount;
    private String currency;
    private DeliveryAddress deliveryAddress;
    private List<Item> items;
    private List<StatusChange> statusHistory;
    private String paymentId;
    private long createdAt;
    private long updatedAt;

    public record Item(Long productId, String productName, long unitPrice, int quantity, long lineTotal) {
    }

    public record StatusChange(OrderStatus status, long changedAt, String note) {
    }

    public static OrderResponseDto from(Order order) {
        OrderResponseDto responseDto = new OrderResponseDto();
        responseDto.setId(order.getId());
        responseDto.setStatus(order.getStatus());
        responseDto.setTotalAmount(order.getTotalAmount());
        responseDto.setCurrency(order.getCurrency());
        responseDto.setDeliveryAddress(order.getDeliveryAddress());
        responseDto.setItems(order.getItems().stream()
                .map(i -> new Item(i.getProductId(), i.getProductName(), i.getUnitPrice(), i.getQuantity(), i.getLineTotal()))
                .toList());
        responseDto.setStatusHistory(order.getStatusHistory().stream()
                .map(s -> new StatusChange(s.getStatus(), s.getChangedAt(), s.getNote()))
                .toList());
        responseDto.setPaymentId(order.getPaymentId());
        responseDto.setCreatedAt(order.getCreatedAt());
        responseDto.setUpdatedAt(order.getUpdatedAt());
        return responseDto;
    }
}
