package com.coffeeflow.backend.dto;

import java.math.BigDecimal;

public class CreateOrderResponse {
    private final String orderId;
    private final String pickupCode;
    private final BigDecimal totalAmount;
    private final String status;

    public CreateOrderResponse(String orderId, String pickupCode, BigDecimal totalAmount, String status) {
        this.orderId = orderId;
        this.pickupCode = pickupCode;
        this.totalAmount = totalAmount;
        this.status = status;
    }

    public String getOrderId() { return orderId; }
    public String getPickupCode() { return pickupCode; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public String getStatus() { return status; }
}
