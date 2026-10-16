package com.ecom.orders.exceptions;

public class InvalidStatusTransitionException extends Exception {
    public InvalidStatusTransitionException(String message) {
        super(message);
    }
}
