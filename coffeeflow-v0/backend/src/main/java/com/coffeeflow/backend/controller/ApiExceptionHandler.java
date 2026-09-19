package com.coffeeflow.backend.controller;

import com.coffeeflow.backend.service.OrderConflictException;
import java.util.Collections;
import java.util.Map;
import java.util.NoSuchElementException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 统一异常到 HTTP 状态码的映射，沿用 V0 语义：
 * NoSuchElementException → 404，IllegalArgumentException → 400，业务冲突 → 409。
 */
@RestControllerAdvice
public class ApiExceptionHandler {

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

    @ExceptionHandler(OrderConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String, String> conflict(OrderConflictException exception) {
        return Collections.singletonMap("message", exception.getMessage());
    }
}
