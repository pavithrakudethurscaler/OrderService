package com.ecom.orders.services;

import com.ecom.orders.clients.ProductClient;
import com.ecom.orders.clients.ProductDto;
import com.ecom.orders.dtos.OrderItemRequestDto;
import com.ecom.orders.exceptions.InvalidOrderException;
import com.ecom.orders.exceptions.InvalidStatusTransitionException;
import com.ecom.orders.exceptions.OrderNotFoundException;
import com.ecom.orders.models.DeliveryAddress;
import com.ecom.orders.models.Order;
import com.ecom.orders.models.OrderStatus;
import com.ecom.orders.repos.OrderRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class OrderServiceTest {
    private OrderRepo orderRepo;
    private ProductClient productClient;
    private ApplicationEventPublisher events;
    private OrderService service;

    @BeforeEach
    void setUp() throws Exception {
        orderRepo = mock(OrderRepo.class);
        productClient = mock(ProductClient.class);
        events = mock(ApplicationEventPublisher.class);
        service = new OrderService(orderRepo, productClient, events, "INR");
        when(orderRepo.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));
        when(productClient.getProduct(1L)).thenReturn(product(1L, "Phone", 499.99));
        when(productClient.getProduct(2L)).thenReturn(product(2L, "Case", 0.10));
        when(productClient.getProduct(999L)).thenThrow(new InvalidOrderException("Product 999 not found"));
    }

    private static ProductDto product(Long id, String name, double price) {
        ProductDto p = new ProductDto();
        p.setId(id);
        p.setName(name);
        p.setPrice(price);
        return p;
    }

    private static OrderItemRequestDto item(Long productId, Integer qty) {
        OrderItemRequestDto i = new OrderItemRequestDto();
        i.setProductId(productId);
        i.setQuantity(qty);
        return i;
    }

    private static DeliveryAddress address() {
        DeliveryAddress a = new DeliveryAddress();
        a.setRecipientName("Alice");
        a.setLine1("1 MG Road");
        a.setCity("Bengaluru");
        a.setPostalCode("560001");
        a.setCountry("IN");
        return a;
    }

    private Order existing(OrderStatus status, String paymentId) {
        Order o = new Order();
        o.setId(1L);
        o.setUserId(7L);
        o.setStatus(status);
        o.setTotalAmount(100028);
        o.setCurrency("INR");
        o.setPaymentId(paymentId);
        when(orderRepo.findById(1L)).thenReturn(Optional.of(o));
        when(orderRepo.findByIdAndUserId(1L, 7L)).thenReturn(Optional.of(o));
        return o;
    }

    @Test
    void placeOrderMergesLinesSnapshotsPricesInPaiseAndComputesTotal() throws Exception {
        Order order = service.placeOrder(7L, List.of(item(1L, 1), item(2L, 3), item(1L, 1)), address());

        assertThat(order.getItems()).hasSize(2);
        assertThat(order.getItems().get(0).getProductName()).isEqualTo("Phone");
        assertThat(order.getItems().get(0).getUnitPrice()).isEqualTo(49999);
        assertThat(order.getItems().get(0).getQuantity()).isEqualTo(2);
        assertThat(order.getItems().get(0).getLineTotal()).isEqualTo(99998);
        assertThat(order.getItems().get(1).getUnitPrice()).isEqualTo(10);
        assertThat(order.getTotalAmount()).isEqualTo(100028);
        assertThat(order.getCurrency()).isEqualTo("INR");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CREATED);
        assertThat(order.getStatusHistory()).extracting("status").containsExactly(OrderStatus.CREATED);
        verify(productClient, times(1)).getProduct(1L);
        // Placing an order is not a customer notification
        verifyNoInteractions(events);
    }

    @Test
    void placeOrderRejectsMergedQuantityOver100() {
        assertThatThrownBy(() -> service.placeOrder(7L, List.of(item(1L, 60), item(1L, 50)), address()))
                .isInstanceOf(InvalidOrderException.class).hasMessageContaining("cannot exceed 100");
    }

    @Test
    void placeOrderValidatesItemsAndAddress() {
        assertThatThrownBy(() -> service.placeOrder(7L, List.of(), address()))
                .isInstanceOf(InvalidOrderException.class).hasMessageContaining("at least one item");
        assertThatThrownBy(() -> service.placeOrder(7L, List.of(item(1L, 0)), address()))
                .isInstanceOf(InvalidOrderException.class).hasMessageContaining("at least 1");
        assertThatThrownBy(() -> service.placeOrder(7L, List.of(item(null, 1)), address()))
                .isInstanceOf(InvalidOrderException.class).hasMessageContaining("productId");
        DeliveryAddress noCity = address();
        noCity.setCity(" ");
        assertThatThrownBy(() -> service.placeOrder(7L, List.of(item(1L, 1)), noCity))
                .isInstanceOf(InvalidOrderException.class).hasMessageContaining("deliveryAddress");
        assertThatThrownBy(() -> service.placeOrder(7L, List.of(item(1L, 1)), null))
                .isInstanceOf(InvalidOrderException.class);
    }

    @Test
    void placeOrderRejectsUnknownProduct() {
        assertThatThrownBy(() -> service.placeOrder(7L, List.of(item(999L, 1)), address()))
                .isInstanceOf(InvalidOrderException.class).hasMessageContaining("999");
        verify(orderRepo, never()).save(any());
    }

    @Test
    void markPaidMovesToPaidAndPublishesConfirmationWithStableId() throws Exception {
        Order order = existing(OrderStatus.CREATED, null);

        service.markPaid(1L, "p1", 100028L);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(order.getPaymentId()).isEqualTo("p1");
        ArgumentCaptor<OrderNotificationEvent> captor = ArgumentCaptor.forClass(OrderNotificationEvent.class);
        verify(events).publishEvent(captor.capture());
        assertThat(captor.getValue().eventId()).isEqualTo("order-1-paid");
        assertThat(captor.getValue().type()).isEqualTo("ORDER_CONFIRMED");
        assertThat(captor.getValue().data()).containsEntry("userId", 7L).containsEntry("totalAmount", 100028L);
    }

    @Test
    void markPaidRejectsAmountMismatchAndMissingPaymentId() {
        existing(OrderStatus.CREATED, null);
        assertThatThrownBy(() -> service.markPaid(1L, "p1", 1L))
                .isInstanceOf(InvalidOrderException.class).hasMessageContaining("does not match");
        assertThatThrownBy(() -> service.markPaid(1L, " ", 100028L)).isInstanceOf(InvalidOrderException.class);
        verifyNoInteractions(events);
    }

    @Test
    void markPaidIsIdempotentForSamePaymentButRejectsASecondPayment() throws Exception {
        Order order = existing(OrderStatus.PAID, "p1");

        assertThat(service.markPaid(1L, "p1", 100028L)).isSameAs(order);
        assertThatThrownBy(() -> service.markPaid(1L, "p2", 100028L))
                .isInstanceOf(InvalidStatusTransitionException.class);
        verify(orderRepo, never()).save(any());
        verifyNoInteractions(events);
    }

    @Test
    void markPaidOnUnknownOrder() {
        when(orderRepo.findById(5L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.markPaid(5L, "p1", 1L)).isInstanceOf(OrderNotFoundException.class);
    }

    @Test
    void updateStatusRejectsManualPaidAndInvalidTransitions() {
        existing(OrderStatus.CREATED, null);
        assertThatThrownBy(() -> service.updateStatus(1L, OrderStatus.PAID, null))
                .isInstanceOf(InvalidOrderException.class).hasMessageContaining("Payment Service");
        assertThatThrownBy(() -> service.updateStatus(1L, null, null)).isInstanceOf(InvalidOrderException.class);
        assertThatThrownBy(() -> service.updateStatus(1L, OrderStatus.SHIPPED, null))
                .isInstanceOf(InvalidStatusTransitionException.class).hasMessageContaining("CREATED to SHIPPED");
        verifyNoInteractions(events);
    }

    @Test
    void shipAndDeliverPublishTrackingEventsWithNote() throws Exception {
        Order order = existing(OrderStatus.PAID, "p1");

        service.updateStatus(1L, OrderStatus.SHIPPED, "AWB 12345");
        service.updateStatus(1L, OrderStatus.DELIVERED, null);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.DELIVERED);
        assertThat(order.getStatusHistory()).extracting("status").containsExactly(OrderStatus.SHIPPED, OrderStatus.DELIVERED);
        ArgumentCaptor<OrderNotificationEvent> captor = ArgumentCaptor.forClass(OrderNotificationEvent.class);
        verify(events, times(2)).publishEvent(captor.capture());
        assertThat(captor.getAllValues()).extracting(OrderNotificationEvent::eventId)
                .containsExactly("order-1-shipped", "order-1-delivered");
        assertThat(captor.getAllValues().get(0).data()).containsEntry("note", "AWB 12345");
    }

    @Test
    void customerCanCancelOnlyOwnUnpaidOrder() throws Exception {
        Order order = existing(OrderStatus.CREATED, null);
        service.cancelOrder(1L, 7L);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        verify(events).publishEvent(any(OrderNotificationEvent.class));

        existing(OrderStatus.PAID, "p1");
        assertThatThrownBy(() -> service.cancelOrder(1L, 7L)).isInstanceOf(InvalidStatusTransitionException.class);

        when(orderRepo.findByIdAndUserId(1L, 8L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.cancelOrder(1L, 8L)).isInstanceOf(OrderNotFoundException.class);
    }

    @Test
    void getOrderIsScopedToTheCustomer() throws Exception {
        existing(OrderStatus.CREATED, null);
        when(orderRepo.findByIdAndUserId(1L, 8L)).thenReturn(Optional.empty());
        assertThat(service.getOrder(1L, 7L).getId()).isEqualTo(1L);
        assertThatThrownBy(() -> service.getOrder(1L, 8L))
                .isInstanceOf(OrderNotFoundException.class).hasMessage("Order not found: 1");
    }
}
