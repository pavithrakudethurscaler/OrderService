package com.ecom.orders.clients;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

/** The fields the Order Service needs from the Product Service's product response. */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProductDto {
    private Long id;
    private String name;
    /** Price in rupees, as stored by the Product Service */
    private double price;
    private boolean deleted;
}
