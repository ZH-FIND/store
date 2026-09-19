package com.coffeeflow.backend.dto;

import java.util.List;

public class OrderListResponse {
    private final int total;
    private final List<OrderSummaryResponse> orders;

    public OrderListResponse(int total, List<OrderSummaryResponse> orders) {
        this.total = total;
        this.orders = orders;
    }

    public int getTotal() { return total; }
    public List<OrderSummaryResponse> getOrders() { return orders; }
}
