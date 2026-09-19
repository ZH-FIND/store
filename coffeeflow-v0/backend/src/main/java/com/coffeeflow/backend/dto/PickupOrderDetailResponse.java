package com.coffeeflow.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** 顾客侧订单详情：按取餐码查询或取消后的返回体。 */
public class PickupOrderDetailResponse {
    private final String orderId;
    private final String pickupCode;
    private final String storeName;
    private final String customerName;
    private final String status;
    private final BigDecimal totalAmount;
    private final LocalDateTime createdAt;
    private final LocalDateTime estimatedReadyAt;
    private final String note;
    private final List<OrderItemDetailResponse> items;

    public PickupOrderDetailResponse(String orderId, String pickupCode, String storeName,
            String customerName, String status, BigDecimal totalAmount, LocalDateTime createdAt,
            LocalDateTime estimatedReadyAt, String note, List<OrderItemDetailResponse> items) {
        this.orderId = orderId;
        this.pickupCode = pickupCode;
        this.storeName = storeName;
        this.customerName = customerName;
        this.status = status;
        this.totalAmount = totalAmount;
        this.createdAt = createdAt;
        this.estimatedReadyAt = estimatedReadyAt;
        this.note = note;
        this.items = items;
    }

    public String getOrderId() { return orderId; }
    public String getPickupCode() { return pickupCode; }
    public String getStoreName() { return storeName; }
    public String getCustomerName() { return customerName; }
    public String getStatus() { return status; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getEstimatedReadyAt() { return estimatedReadyAt; }
    public String getNote() { return note; }
    public List<OrderItemDetailResponse> getItems() { return items; }
}
