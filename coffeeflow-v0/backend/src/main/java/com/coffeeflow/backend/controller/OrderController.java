package com.coffeeflow.backend.controller;

import com.coffeeflow.backend.dto.OrderDetailResponse;
import com.coffeeflow.backend.dto.OrderListResponse;
import com.coffeeflow.backend.dto.UpdateOrderStatusRequest;
import com.coffeeflow.backend.model.OrderStatus;
import com.coffeeflow.backend.service.OrderService;
import java.util.Collections;
import java.util.Map;
import java.util.NoSuchElementException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {
    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public OrderListResponse getOrders(@RequestParam(required = false) OrderStatus status) {
        return orderService.getOrders(status);
    }

    @GetMapping("/{orderId}")
    public OrderDetailResponse getOrder(@PathVariable String orderId) {
        return orderService.getOrderById(orderId);
    }

    @PatchMapping("/{orderId}/status")
    public OrderDetailResponse updateStatus(@PathVariable String orderId,
            @RequestBody UpdateOrderStatusRequest request) {
        return orderService.updateOrderStatus(orderId, request.getStatus());
    }

    @ExceptionHandler(NoSuchElementException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, String> notFound(NoSuchElementException exception) {
        return Collections.singletonMap("message", exception.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> badRequest(IllegalArgumentException exception) {
        return Collections.singletonMap("message", exception.getMessage());
    }
}
