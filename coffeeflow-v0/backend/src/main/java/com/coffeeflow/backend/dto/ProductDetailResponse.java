package com.coffeeflow.backend.dto;

import java.math.BigDecimal;

public class ProductDetailResponse {
    private final String productCode;
    private final String name;
    private final BigDecimal price;
    private final Boolean available;

    public ProductDetailResponse(String productCode, String name, BigDecimal price, Boolean available) {
        this.productCode = productCode;
        this.name = name;
        this.price = price;
        this.available = available;
    }

    public String getProductCode() { return productCode; }
    public String getName() { return name; }
    public BigDecimal getPrice() { return price; }
    public Boolean getAvailable() { return available; }
}
