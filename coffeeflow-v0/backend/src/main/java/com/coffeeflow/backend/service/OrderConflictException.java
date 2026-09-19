package com.coffeeflow.backend.service;

/**
 * 业务冲突（如取消已开始制作的订单），映射为 HTTP 409。
 */
public class OrderConflictException extends RuntimeException {
    public OrderConflictException(String message) {
        super(message);
    }
}
