package com.ecom.orders.services;

import java.util.Map;

/** Raised inside an order transaction; sent to the Notification Service only after the transaction commits. */
public record OrderNotificationEvent(String eventId, String type, Map<String, Object> data) {
}
