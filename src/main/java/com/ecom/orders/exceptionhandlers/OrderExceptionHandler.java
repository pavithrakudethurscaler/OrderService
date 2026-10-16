package com.ecom.orders.exceptionhandlers;

import com.ecom.common.dtos.ErrorDto;
import com.ecom.orders.auth.AuthServiceUnavailableException;
import com.ecom.orders.auth.UnauthorizedException;
import com.ecom.orders.clients.DownstreamServiceException;
import com.ecom.orders.exceptions.InvalidOrderException;
import com.ecom.orders.exceptions.InvalidStatusTransitionException;
import com.ecom.orders.exceptions.OrderNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class OrderExceptionHandler {
    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<ErrorDto> handleOrderNotFound(OrderNotFoundException e) {
        return new ResponseEntity<>(new ErrorDto(e.getMessage()), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(InvalidOrderException.class)
    public ResponseEntity<ErrorDto> handleInvalidOrder(InvalidOrderException e) {
        return new ResponseEntity<>(new ErrorDto(e.getMessage()), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(InvalidStatusTransitionException.class)
    public ResponseEntity<ErrorDto> handleInvalidStatusTransition(InvalidStatusTransitionException e) {
        return new ResponseEntity<>(new ErrorDto(e.getMessage()), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorDto> handleUnauthorized(UnauthorizedException e) {
        return new ResponseEntity<>(new ErrorDto(e.getMessage()), HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler({AuthServiceUnavailableException.class, DownstreamServiceException.class})
    public ResponseEntity<ErrorDto> handleServiceUnavailable(RuntimeException e) {
        return new ResponseEntity<>(new ErrorDto(e.getMessage()), HttpStatus.SERVICE_UNAVAILABLE);
    }
}
