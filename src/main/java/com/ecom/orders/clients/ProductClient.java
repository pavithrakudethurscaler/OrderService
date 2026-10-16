package com.ecom.orders.clients;

import com.ecom.orders.exceptions.InvalidOrderException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/** Reads product name and current price from the Product Service. */
@Component
public class ProductClient {
    private final RestTemplate restTemplate;
    private final String productServiceBaseUrl;

    public ProductClient(RestTemplate restTemplate, @Value("${productservice.base-url}") String productServiceBaseUrl) {
        this.restTemplate = restTemplate;
        this.productServiceBaseUrl = productServiceBaseUrl;
    }

    public ProductDto getProduct(Long productId) throws InvalidOrderException {
        try {
            ProductDto product = restTemplate.getForObject(
                    productServiceBaseUrl + "/api/v1/products/{id}", ProductDto.class, productId);
            if (product == null || product.isDeleted()) {
                throw new InvalidOrderException("Product " + productId + " not found");
            }
            return product;
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new InvalidOrderException("Product " + productId + " not found");
            }
            throw new DownstreamServiceException("Product Service rejected the request", e);
        } catch (RestClientException e) {
            throw new DownstreamServiceException("Product Service is unavailable", e);
        }
    }
}
