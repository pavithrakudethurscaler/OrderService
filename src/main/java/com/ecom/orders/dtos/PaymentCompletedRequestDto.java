package com.ecom.orders.dtos;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PaymentCompletedRequestDto {
    private String paymentId;
    /** Amount actually paid, in paise; must equal the order total */
    private Long amount;
}
