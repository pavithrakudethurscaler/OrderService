package com.ecom.orders.models;

import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Embeddable
public class DeliveryAddress {
    private String recipientName;
    private String phoneNumber;
    private String line1;
    private String line2;
    private String city;
    private String state;
    private String postalCode;
    private String country;
}
