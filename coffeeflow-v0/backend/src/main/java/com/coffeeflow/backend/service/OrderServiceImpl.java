package com.coffeeflow.backend.service;

import com.coffeeflow.backend.dto.CancelOrderRequest;
import com.coffeeflow.backend.dto.CreateOrderRequest;
import com.coffeeflow.backend.dto.CreateOrderResponse;
import com.coffeeflow.backend.dto.OrderDetailResponse;
import com.coffeeflow.backend.dto.OrderItemDetailResponse;
import com.coffeeflow.backend.dto.OrderListResponse;
import com.coffeeflow.backend.dto.OrderSummaryResponse;
import com.coffeeflow.backend.dto.PickupOrderDetailResponse;
import com.coffeeflow.backend.model.Order;
import com.coffeeflow.backend.model.OrderItem;
import com.coffeeflow.backend.model.OrderStatus;
import com.coffeeflow.backend.model.Product;
import com.coffeeflow.backend.model.Store;
import com.coffeeflow.backend.repository.OrderRepository;
import com.coffeeflow.backend.repository.ProductRepository;
import com.coffeeflow.backend.repository.StoreRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderServiceImpl implements OrderService {
    /** 取餐码为 4 位数字，当日首单从 1001 开始。 */
    private static final int FIRST_PICKUP_CODE = 1001;
    /** 预计取餐时间按下单时间 + 15 分钟生成（占位值，不参与业务规则）。 */
    private static final int ESTIMATED_READY_MINUTES = 15;
    private static final String DEFAULT_SIZE = "中杯";

    private final OrderRepository orderRepository;
    private final StoreRepository storeRepository;
    private final ProductRepository productRepository;

    public OrderServiceImpl(OrderRepository orderRepository, StoreRepository storeRepository,
            ProductRepository productRepository) {
        this.orderRepository = orderRepository;
        this.storeRepository = storeRepository;
        this.productRepository = productRepository;
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
        findOrder(orderId);
        orderRepository.updateStatus(orderId, status);
        return toDetail(findOrder(orderId));
    }

    @Override
    @Transactional
    public CreateOrderResponse createOrder(CreateOrderRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Order request must not be null");
        }
        String storeId = request.getStoreId();
        if (storeId == null || storeId.isBlank()) {
            throw new IllegalArgumentException("Store id must not be blank");
        }
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new IllegalArgumentException("Store not found: " + storeId));
        String customerName = request.getCustomerName();
        if (customerName == null || customerName.isBlank()) {
            throw new IllegalArgumentException("Customer name must not be blank");
        }
        String phoneLast4 = request.getPhoneLast4();
        if (phoneLast4 == null || !phoneLast4.matches("\\d{4}")) {
            throw new IllegalArgumentException("Phone last 4 digits must be 4 digits");
        }
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new IllegalArgumentException("Order items must not be empty");
        }

        LocalDateTime createdAt = LocalDateTime.now();
        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;
        int lineNo = 1;
        for (CreateOrderRequest.Item requested : request.getItems()) {
            String productCode = requested.getProductCode();
            if (productCode == null || productCode.isBlank()) {
                throw new IllegalArgumentException("Product code must not be blank");
            }
            Integer quantity = requested.getQuantity();
            if (quantity == null || quantity < 1) {
                throw new IllegalArgumentException(
                        "Quantity must be at least 1 for product: " + productCode);
            }
            Product product = productRepository.findById(productCode)
                    .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productCode));
            Product storeProduct = productRepository.findStoreProduct(store.getStoreId(), productCode)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Product is not available in store " + store.getStoreId() + ": " + productCode));
            if (!Boolean.TRUE.equals(storeProduct.getAvailable())) {
                throw new IllegalArgumentException(
                        "Product is not available in store " + store.getStoreId() + ": " + productCode);
            }

            // 单价一律取商品价，规格仅作展示属性，不参与计价
            BigDecimal unitPrice = product.getPrice();
            BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
            String size = requested.getSize() == null || requested.getSize().isBlank()
                    ? DEFAULT_SIZE : requested.getSize();
            orderItems.add(new OrderItem(lineNo++, product.getProductCode(), product.getName(),
                    size, quantity, unitPrice, subtotal));
            totalAmount = totalAmount.add(subtotal);
        }

        LocalDate pickupDate = createdAt.toLocalDate();
        int pickupCode = orderRepository.findMaxPickupCodeOnDate(pickupDate)
                .orElse(FIRST_PICKUP_CODE - 1) + 1;
        String orderId = OrderRepository.ORDER_ID_PREFIX + orderRepository.nextOrderSequence();
        Order order = new Order(orderId, store.getStoreName(), customerName, OrderStatus.NEW,
                createdAt, createdAt.plusMinutes(ESTIMATED_READY_MINUTES),
                request.getNote() == null ? "" : request.getNote(), phoneLast4,
                String.valueOf(pickupCode), pickupDate, totalAmount, orderItems);
        orderRepository.save(order);
        return new CreateOrderResponse(orderId, String.valueOf(pickupCode), totalAmount,
                OrderStatus.NEW.name());
    }

    @Override
    @Transactional(readOnly = true)
    public PickupOrderDetailResponse getOrderByPickupCode(String pickupCode) {
        Order order = orderRepository.findByPickupCodeOnDate(pickupCode, LocalDate.now())
                .orElseThrow(() -> new NoSuchElementException(
                        "Order not found for pickup code: " + pickupCode));
        return toPickupDetail(order);
    }

    @Override
    @Transactional
    public PickupOrderDetailResponse cancelOrder(String orderId, CancelOrderRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Cancel request must not be null");
        }
        Order order = findOrder(orderId);
        if (request.getPhoneLast4() == null || !request.getPhoneLast4().equals(order.getPhoneLast4())) {
            throw new IllegalArgumentException("Phone last 4 digits do not match order: " + orderId);
        }
        if (request.getPickupCode() == null || !request.getPickupCode().equals(order.getPickupCode())) {
            throw new IllegalArgumentException("Pickup code does not match order: " + orderId);
        }
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new OrderConflictException("Order already cancelled: " + orderId);
        }
        if (order.getStatus() != OrderStatus.NEW) {
            throw new OrderConflictException(
                    "Order cannot be cancelled in status " + order.getStatus() + ": " + orderId);
        }
        orderRepository.updateStatus(orderId, OrderStatus.CANCELLED);
        return toPickupDetail(findOrder(orderId));
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

    private PickupOrderDetailResponse toPickupDetail(Order order) {
        List<OrderItemDetailResponse> items = order.getOrderItems().stream()
                .map(item -> new OrderItemDetailResponse(item.getProductCode(), item.getDrinkName(),
                        item.getSize(), item.getQuantity(), item.getUnitPrice(), item.getSubtotal()))
                .collect(Collectors.toList());
        return new PickupOrderDetailResponse(order.getId(), order.getPickupCode(),
                order.getStoreName(), order.getCustomerName(), order.getStatus().name(),
                order.getTotalAmount(), order.getCreatedAt(), order.getEstimatedReadyAt(),
                order.getNote(), items);
    }

    private Order findOrder(String orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new NoSuchElementException("Order not found: " + orderId));
    }
}
