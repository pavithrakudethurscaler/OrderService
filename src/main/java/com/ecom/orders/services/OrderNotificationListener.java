package com.ecom.orders.services;

import com.ecom.orders.clients.NotificationPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Sends order notifications after commit, so a customer is never told about a status change that was rolled back
 * (for example when a concurrent update wins the optimistic lock).
 */
@Component
public class OrderNotificationListener {
    private final NotificationPublisher notificationPublisher;

    public OrderNotificationListener(NotificationPublisher notificationPublisher) {
        this.notificationPublisher = notificationPublisher;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderNotification(OrderNotificationEvent event) {
        notificationPublisher.publish(event.eventId(), event.type(), event.data());
    }
}
