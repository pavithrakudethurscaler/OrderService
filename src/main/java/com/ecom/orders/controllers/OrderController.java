package com.ecom.orders.controllers;

import com.ecom.orders.auth.AuthInterceptor;
import com.ecom.orders.auth.AuthUserDto;
import com.ecom.orders.dtos.CreateOrderRequestDto;
import com.ecom.orders.dtos.OrderResponseDto;
import com.ecom.orders.exceptions.InvalidOrderException;
import com.ecom.orders.exceptions.InvalidStatusTransitionException;
import com.ecom.orders.exceptions.OrderNotFoundException;
import com.ecom.orders.models.Order;
import com.ecom.orders.services.OrderService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {
    OrderService orderService;

    OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<OrderResponseDto> placeOrder(
            @RequestBody CreateOrderRequestDto createOrderRequestDto,
            @RequestAttribute(AuthInterceptor.AUTH_USER_ATTRIBUTE) AuthUserDto user) throws InvalidOrderException {
        Order order = orderService.placeOrder(user.getId(),
                createOrderRequestDto.getItems(),
                createOrderRequestDto.getDeliveryAddress());
        return new ResponseEntity<>(OrderResponseDto.from(order), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<OrderResponseDto>> getOrders(
            @RequestAttribute(AuthInterceptor.AUTH_USER_ATTRIBUTE) AuthUserDto user) {
        List<OrderResponseDto> orders = orderService.getOrders(user.getId()).stream()
                .map(OrderResponseDto::from)
                .toList();
        return new ResponseEntity<>(orders, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponseDto> getOrder(
            @PathVariable Long id,
            @RequestAttribute(AuthInterceptor.AUTH_USER_ATTRIBUTE) AuthUserDto user) throws OrderNotFoundException {
        return new ResponseEntity<>(OrderResponseDto.from(orderService.getOrder(id, user.getId())), HttpStatus.OK);
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<OrderResponseDto> cancelOrder(
            @PathVariable Long id,
            @RequestAttribute(AuthInterceptor.AUTH_USER_ATTRIBUTE) AuthUserDto user)
            throws OrderNotFoundException, InvalidStatusTransitionException {
        return new ResponseEntity<>(OrderResponseDto.from(orderService.cancelOrder(id, user.getId())), HttpStatus.OK);
    }
}
