package com.coffeeflow.backend.service;

import com.coffeeflow.backend.dto.OrderDetailResponse;
import com.coffeeflow.backend.dto.OrderListResponse;
import com.coffeeflow.backend.model.OrderStatus;

public interface OrderService {
    OrderListResponse getOrders(OrderStatus status);
    OrderDetailResponse getOrderById(String orderId);
    OrderDetailResponse updateOrderStatus(String orderId, OrderStatus status);
}
