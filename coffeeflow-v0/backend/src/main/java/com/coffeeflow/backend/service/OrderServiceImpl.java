package com.coffeeflow.backend.service;

import com.coffeeflow.backend.dto.OrderDetailResponse;
import com.coffeeflow.backend.dto.OrderListResponse;
import com.coffeeflow.backend.dto.OrderSummaryResponse;
import com.coffeeflow.backend.model.Order;
import com.coffeeflow.backend.model.OrderStatus;
import com.coffeeflow.backend.repository.OrderRepository;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderServiceImpl implements OrderService {
    private final OrderRepository orderRepository;

    public OrderServiceImpl(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public OrderListResponse getOrders(OrderStatus status) {
        List<Order> found = status == null ? orderRepository.findAll()
                : orderRepository.findAllByStatus(status);
        List<OrderSummaryResponse> orders = found.stream().map(this::toSummary)
                .collect(Collectors.toList());
        return new OrderListResponse(orders.size(), orders);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderDetailResponse getOrderById(String orderId) {
        return toDetail(findOrder(orderId));
    }

    @Override
    @Transactional
    public OrderDetailResponse updateOrderStatus(String orderId, OrderStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("Order status must not be null");
        }
        Order order = findOrder(orderId);
        order.setStatus(status);
        return toDetail(orderRepository.save(order));
    }

    private OrderSummaryResponse toSummary(Order order) {
        return new OrderSummaryResponse(order.getId(), order.getStoreName(),
                order.getCustomerName(), order.getProductCode(),
                order.getDrinkName(), order.getItems(), order.getQuantity(),
                order.getStatus().name(), order.getCreatedAt(),
                order.getEstimatedReadyAt(), order.getNote());
    }

    private OrderDetailResponse toDetail(Order order) {
        return new OrderDetailResponse(order.getId(), order.getStoreName(),
                order.getCustomerName(), order.getProductCode(),
                order.getDrinkName(), order.getItems(), order.getSize(),
                order.getQuantity(), order.getStatus().name(), order.getCreatedAt(),
                order.getEstimatedReadyAt(), order.getNote());
    }

    private Order findOrder(String orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new NoSuchElementException("Order not found: " + orderId));
    }
}
