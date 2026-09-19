package com.coffeeflow.backend.controller;

import com.coffeeflow.backend.dto.CancelOrderRequest;
import com.coffeeflow.backend.dto.CreateOrderRequest;
import com.coffeeflow.backend.dto.CreateOrderResponse;
import com.coffeeflow.backend.dto.OrderDetailResponse;
import com.coffeeflow.backend.dto.OrderListResponse;
import com.coffeeflow.backend.dto.PickupOrderDetailResponse;
import com.coffeeflow.backend.dto.UpdateOrderStatusRequest;
import com.coffeeflow.backend.model.OrderStatus;
import com.coffeeflow.backend.service.OrderService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreateOrderResponse createOrder(@RequestBody CreateOrderRequest request) {
        return orderService.createOrder(request);
    }

    /** 取餐码只在当日范围内有效，历史订单的取餐码为空，查不到。 */
    @GetMapping("/pickup/{pickupCode}")
    public PickupOrderDetailResponse getOrderByPickupCode(@PathVariable String pickupCode) {
        return orderService.getOrderByPickupCode(pickupCode);
    }

    @PostMapping("/{orderId}/cancel")
    public PickupOrderDetailResponse cancelOrder(@PathVariable String orderId,
            @RequestBody CancelOrderRequest request) {
        return orderService.cancelOrder(orderId, request);
    }
}
