package com.coffeeflow.backend.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * 订单头。商品信息不再压在订单行上，而是由 {@link OrderItem} 明细聚合得出。
 */
public class Order {
    private final String id;
    private final String storeName;
    private final String customerName;
    private OrderStatus status;
    private final LocalDateTime createdAt;
    private final LocalDateTime estimatedReadyAt;
    private final String note;
    private final String phoneLast4;
    private final String pickupCode;
    private final LocalDate pickupDate;
    private final BigDecimal totalAmount;
    private final List<OrderItem> orderItems;

    public Order(String id, String storeName, String customerName, OrderStatus status,
                 LocalDateTime createdAt, LocalDateTime estimatedReadyAt, String note,
                 String phoneLast4, String pickupCode, LocalDate pickupDate,
                 BigDecimal totalAmount, List<OrderItem> orderItems) {
        this.id = id;
        this.storeName = storeName;
        this.customerName = customerName;
        this.status = status;
        this.createdAt = createdAt;
        this.estimatedReadyAt = estimatedReadyAt;
        this.note = note;
        this.phoneLast4 = phoneLast4;
        this.pickupCode = pickupCode;
        this.pickupDate = pickupDate;
        this.totalAmount = totalAmount;
        this.orderItems = new ArrayList<>(orderItems);
    }

    public String getId() { return id; }
    public String getStoreName() { return storeName; }
    public String getCustomerName() { return customerName; }
    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getEstimatedReadyAt() { return estimatedReadyAt; }
    public String getNote() { return note; }
    public String getPhoneLast4() { return phoneLast4; }
    public String getPickupCode() { return pickupCode; }
    public LocalDate getPickupDate() { return pickupDate; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public List<OrderItem> getOrderItems() { return Collections.unmodifiableList(orderItems); }

    /** V0 契约映射：主商品取第一条明细（按 line_no 升序）。 */
    public String getProductCode() {
        return firstItem().map(OrderItem::getProductCode).orElse(null);
    }

    /** V0 契约映射：主商品名取第一条明细。 */
    public String getDrinkName() {
        return firstItem().map(OrderItem::getDrinkName).orElse(null);
    }

    /** V0 契约映射：规格取第一条明细。 */
    public String getSize() {
        return firstItem().map(OrderItem::getSize).orElse(null);
    }

    /** V0 契约映射：quantity 为明细条目数，不是任一商品的杯数。 */
    public int getQuantity() {
        return orderItems.size();
    }

    /** V0 契约映射：items 为全部明细的饮品名列表，顺序即 line_no 顺序。 */
    public List<String> getItems() {
        return orderItems.stream().map(OrderItem::getDrinkName).toList();
    }

    private Optional<OrderItem> firstItem() {
        return orderItems.stream().findFirst();
    }
}
