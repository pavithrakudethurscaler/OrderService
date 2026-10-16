package com.ecom.orders.services;

import com.ecom.orders.clients.ProductClient;
import com.ecom.orders.clients.ProductDto;
import com.ecom.orders.dtos.OrderItemRequestDto;
import com.ecom.orders.exceptions.InvalidOrderException;
import com.ecom.orders.exceptions.InvalidStatusTransitionException;
import com.ecom.orders.exceptions.OrderNotFoundException;
import com.ecom.orders.models.DeliveryAddress;
import com.ecom.orders.models.Order;
import com.ecom.orders.models.OrderItem;
import com.ecom.orders.models.OrderStatus;
import com.ecom.orders.models.OrderStatusChange;
import com.ecom.orders.repos.OrderRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class OrderService {
    private static final Logger log = LoggerFactory.getLogger(OrderService.class);
    private static final int MAX_QUANTITY_PER_ITEM = 100;

    private final OrderRepo orderRepo;
    private final ProductClient productClient;
    private final String currency;

    public OrderService(OrderRepo orderRepo, ProductClient productClient,
                        @Value("${order.currency:INR}") String currency) {
        this.orderRepo = orderRepo;
        this.productClient = productClient;
        this.currency = currency;
    }

    /*
     1. Validate items and address
     2. Fetch each product's current name and price from the Product Service
     3. Copy them onto the order and calculate the total on the server
     */
    @Transactional
    public Order placeOrder(Long userId, List<OrderItemRequestDto> items, DeliveryAddress address)
            throws InvalidOrderException {
        validateAddress(address);
        Map<Long, Integer> quantities = mergeItems(items);

        long now = System.currentTimeMillis();
        Order order = new Order();
        order.setUserId(userId);
        order.setCurrency(currency);
        order.setDeliveryAddress(address);
        order.setCreatedAt(now);

        long total = 0;
        for (Map.Entry<Long, Integer> entry : quantities.entrySet()) {
            ProductDto product = productClient.getProduct(entry.getKey());
            OrderItem item = new OrderItem();
            item.setOrder(order);
            item.setProductId(product.getId());
            item.setProductName(product.getName());
            item.setUnitPrice(rupeesToPaise(product.getPrice()));
            item.setQuantity(entry.getValue());
            item.setLineTotal(Math.multiplyExact(item.getUnitPrice(), (long) item.getQuantity()));
            order.getItems().add(item);
            total = Math.addExact(total, item.getLineTotal());
        }
        if (total <= 0) {
            throw new InvalidOrderException("Order total must be greater than zero");
        }
        order.setTotalAmount(total);
        changeStatus(order, OrderStatus.CREATED, null, now);
        return orderRepo.save(order);
    }

    @Transactional(readOnly = true)
    public List<Order> getOrders(Long userId) {
        return orderRepo.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional(readOnly = true)
    public Order getOrder(Long id, Long userId) throws OrderNotFoundException {
        // Customers only see their own orders; others get the same 404 as a missing order
        return orderRepo.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new OrderNotFoundException("Order not found: " + id));
    }

    @Transactional
    public Order cancelOrder(Long id, Long userId) throws OrderNotFoundException, InvalidStatusTransitionException {
        Order order = getOrder(id, userId);
        transition(order, OrderStatus.CANCELLED, "Cancelled by customer");
        return orderRepo.save(order);
    }

    /**
     * Called by the Payment Service when a payment completes. Idempotent: repeating the call for the
     * same payment is a no-op, so the Payment Service can safely retry.
     */
    @Transactional
    public Order markPaid(Long id, String paymentId, Long amount)
            throws OrderNotFoundException, InvalidOrderException, InvalidStatusTransitionException {
        Order order = orderRepo.findById(id).orElseThrow(() -> new OrderNotFoundException("Order not found: " + id));
        if (paymentId == null || paymentId.isBlank()) {
            throw new InvalidOrderException("paymentId is required");
        }
        if (order.getStatus() != OrderStatus.CREATED && paymentId.equals(order.getPaymentId())) {
            return order;
        }
        if (amount == null || amount != order.getTotalAmount()) {
            throw new InvalidOrderException("Paid amount " + amount + " does not match order total " + order.getTotalAmount());
        }
        transition(order, OrderStatus.PAID, "Payment " + paymentId);
        order.setPaymentId(paymentId);
        // TODO: publish OrderConfirmed to Kafka for the Notification Service (PRD 4.1, HLD 3.6)
        log.info("Order {} paid by payment {}", order.getId(), paymentId);
        return orderRepo.save(order);
    }

    /** Fulfilment updates (SHIPPED, DELIVERED) from warehouse / ops tools. PAID can only be set by markPaid. */
    @Transactional
    public Order updateStatus(Long id, OrderStatus status, String note)
            throws OrderNotFoundException, InvalidOrderException, InvalidStatusTransitionException {
        if (status == null) {
            throw new InvalidOrderException("status is required");
        }
        if (status == OrderStatus.PAID) {
            throw new InvalidOrderException("PAID is set by the Payment Service, not manually");
        }
        Order order = orderRepo.findById(id).orElseThrow(() -> new OrderNotFoundException("Order not found: " + id));
        transition(order, status, note);
        return orderRepo.save(order);
    }

    private void transition(Order order, OrderStatus next, String note) throws InvalidStatusTransitionException {
        if (!order.getStatus().canMoveTo(next)) {
            throw new InvalidStatusTransitionException(
                    "Order " + order.getId() + " cannot move from " + order.getStatus() + " to " + next);
        }
        changeStatus(order, next, note, System.currentTimeMillis());
    }

    private void changeStatus(Order order, OrderStatus status, String note, long now) {
        order.setStatus(status);
        order.setUpdatedAt(now);
        OrderStatusChange change = new OrderStatusChange();
        change.setOrder(order);
        change.setStatus(status);
        change.setChangedAt(now);
        change.setNote(note);
        order.getStatusHistory().add(change);
    }

    // Combine repeated lines for the same product and validate quantities
    private Map<Long, Integer> mergeItems(List<OrderItemRequestDto> items) throws InvalidOrderException {
        if (items == null || items.isEmpty()) {
            throw new InvalidOrderException("Order must contain at least one item");
        }
        Map<Long, Integer> quantities = new LinkedHashMap<>();
        for (OrderItemRequestDto item : items) {
            if (item.getProductId() == null) {
                throw new InvalidOrderException("Each item needs a productId");
            }
            if (item.getQuantity() == null || item.getQuantity() < 1) {
                throw new InvalidOrderException("Quantity for product " + item.getProductId() + " must be at least 1");
            }
            int merged = quantities.merge(item.getProductId(), item.getQuantity(), Integer::sum);
            if (merged > MAX_QUANTITY_PER_ITEM) {
                throw new InvalidOrderException("Quantity for product " + item.getProductId()
                        + " cannot exceed " + MAX_QUANTITY_PER_ITEM);
            }
        }
        return quantities;
    }

    private void validateAddress(DeliveryAddress address) throws InvalidOrderException {
        if (address == null || isBlank(address.getRecipientName()) || isBlank(address.getLine1())
                || isBlank(address.getCity()) || isBlank(address.getPostalCode()) || isBlank(address.getCountry())) {
            throw new InvalidOrderException(
                    "deliveryAddress needs recipientName, line1, city, postalCode and country");
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static long rupeesToPaise(double rupees) {
        return BigDecimal.valueOf(rupees).movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValueExact();
    }
}
