package com.ecom.orders.controllers;

import com.ecom.orders.dtos.OrderResponseDto;
import com.ecom.orders.dtos.PaymentCompletedRequestDto;
import com.ecom.orders.dtos.UpdateOrderStatusRequestDto;
import com.ecom.orders.exceptions.InvalidOrderException;
import com.ecom.orders.exceptions.InvalidStatusTransitionException;
import com.ecom.orders.exceptions.OrderNotFoundException;
import com.ecom.orders.services.OrderService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Service-to-service endpoints, protected by the X-Internal-Token header (see InternalApiInterceptor). */
@RestController
@RequestMapping("/internal/api/v1/orders")
public class InternalOrderController {
    OrderService orderService;

    InternalOrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/{id}/payment-completed")
    public ResponseEntity<OrderResponseDto> paymentCompleted(
            @PathVariable Long id, @RequestBody PaymentCompletedRequestDto paymentCompletedRequestDto)
            throws OrderNotFoundException, InvalidOrderException, InvalidStatusTransitionException {
        return new ResponseEntity<>(OrderResponseDto.from(orderService.markPaid(id,
                paymentCompletedRequestDto.getPaymentId(),
                paymentCompletedRequestDto.getAmount())), HttpStatus.OK);
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<OrderResponseDto> updateStatus(
            @PathVariable Long id, @RequestBody UpdateOrderStatusRequestDto updateOrderStatusRequestDto)
            throws OrderNotFoundException, InvalidOrderException, InvalidStatusTransitionException {
        return new ResponseEntity<>(OrderResponseDto.from(orderService.updateStatus(id,
                updateOrderStatusRequestDto.getStatus(),
                updateOrderStatusRequestDto.getNote())), HttpStatus.OK);
    }
}
