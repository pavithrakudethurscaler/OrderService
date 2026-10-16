package com.ecom.orders.controllers;

import com.ecom.orders.auth.AuthInterceptor;
import com.ecom.orders.auth.AuthUserDto;
import com.ecom.orders.auth.ForbiddenException;
import com.ecom.orders.dtos.OrderResponseDto;
import com.ecom.orders.dtos.UpdateOrderStatusRequestDto;
import com.ecom.orders.exceptions.InvalidOrderException;
import com.ecom.orders.exceptions.InvalidStatusTransitionException;
import com.ecom.orders.exceptions.OrderNotFoundException;
import com.ecom.orders.models.OrderStatus;
import com.ecom.orders.services.OrderService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Order management for administrators (user token with the ADMIN role).
 * Warehouse systems can keep using the internal endpoints with X-Internal-Token.
 */
@RestController
@RequestMapping("/api/v1/admin/orders")
public class AdminOrderController {
    private static final String ADMIN_ROLE = "ADMIN";

    OrderService orderService;

    AdminOrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    /** All customers' orders, newest first, optionally filtered by status */
    @GetMapping
    public ResponseEntity<List<OrderResponseDto>> listOrders(
            @RequestParam(required = false) OrderStatus status,
            @RequestAttribute(AuthInterceptor.AUTH_USER_ATTRIBUTE) AuthUserDto user) {
        requireAdmin(user);
        List<OrderResponseDto> orders = orderService.getAllOrders(status).stream()
                .map(OrderResponseDto::from)
                .toList();
        return new ResponseEntity<>(orders, HttpStatus.OK);
    }

    /** Fulfilment updates (SHIPPED, DELIVERED) or cancelling an unpaid order; PAID is rejected */
    @PutMapping("/{id}/status")
    public ResponseEntity<OrderResponseDto> updateStatus(
            @PathVariable Long id,
            @RequestBody UpdateOrderStatusRequestDto updateOrderStatusRequestDto,
            @RequestAttribute(AuthInterceptor.AUTH_USER_ATTRIBUTE) AuthUserDto user)
            throws OrderNotFoundException, InvalidOrderException, InvalidStatusTransitionException {
        requireAdmin(user);
        return new ResponseEntity<>(OrderResponseDto.from(orderService.updateStatus(id,
                updateOrderStatusRequestDto.getStatus(),
                updateOrderStatusRequestDto.getNote())), HttpStatus.OK);
    }

    private static void requireAdmin(AuthUserDto user) {
        if (user.getRoles() == null || !user.getRoles().contains(ADMIN_ROLE)) {
            throw new ForbiddenException("Administrator role required");
        }
    }
}
