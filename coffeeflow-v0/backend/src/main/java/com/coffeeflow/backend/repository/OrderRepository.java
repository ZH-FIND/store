package com.coffeeflow.backend.repository;

import com.coffeeflow.backend.model.Order;
import com.coffeeflow.backend.model.OrderItem;
import com.coffeeflow.backend.model.OrderStatus;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/**
 * 订单头与订单明细的聚合读写。
 * 明细统一按 line_no 升序还原，保证 V0 契约（主商品取第一条明细、items 顺序）稳定。
 */
@Repository
public class OrderRepository {
    public static final String ORDER_ID_PREFIX = "CF-";

    private static final String SELECT_HEAD_COLUMNS =
            "SELECT id, store_name, customer_name, status, created_at, estimated_ready_at, note, "
                    + "phone_last4, pickup_code, pickup_date, total_amount FROM orders";

    private static final String SELECT_ITEMS_PREFIX =
            "SELECT order_id, line_no, product_code, drink_name, size, quantity, unit_price, subtotal "
                    + "FROM order_items WHERE order_id IN (";

    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<Order> headMapper = this::mapHead;

    public OrderRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public long count() {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM orders", Long.class);
        return count == null ? 0 : count;
    }

    public List<Order> findAll() {
        return withItems(jdbcTemplate.query(SELECT_HEAD_COLUMNS + " ORDER BY id", headMapper));
    }

    public List<Order> findAllByStatus(OrderStatus status) {
        return withItems(jdbcTemplate.query(SELECT_HEAD_COLUMNS + " WHERE status = ? ORDER BY id",
                headMapper, status.name()));
    }

    public Optional<Order> findById(String id) {
        return withItems(jdbcTemplate.query(SELECT_HEAD_COLUMNS + " WHERE id = ?", headMapper, id))
                .stream().findFirst();
    }

    /** 取餐码只在「当日 + 取餐码非空」范围内匹配，历史订单（取餐码为空）天然查不到。 */
    public Optional<Order> findByPickupCodeOnDate(String pickupCode, LocalDate pickupDate) {
        return withItems(jdbcTemplate.query(
                SELECT_HEAD_COLUMNS + " WHERE pickup_code = ? AND pickup_date = ?",
                headMapper, pickupCode, Date.valueOf(pickupDate)))
                .stream().findFirst();
    }

    /** 下一个订单序号：取既有 CF- 订单号的最大序号 + 1，避免与历史订单冲突。 */
    public int nextOrderSequence() {
        List<String> ids = jdbcTemplate.queryForList("SELECT id FROM orders", String.class);
        int max = 0;
        for (String id : ids) {
            if (id == null || !id.startsWith(ORDER_ID_PREFIX)) {
                continue;
            }
            try {
                max = Math.max(max, Integer.parseInt(id.substring(ORDER_ID_PREFIX.length())));
            } catch (NumberFormatException ignored) {
                // 非「CF-数字」形式的订单号不参与序号计算
            }
        }
        return max + 1;
    }

    /**
     * 当日已用的最大取餐码。
     * 取餐码固定 4 位数字，字符串最大值即数值最大值。
     */
    public Optional<Integer> findMaxPickupCodeOnDate(LocalDate pickupDate) {
        String max = jdbcTemplate.queryForObject(
                "SELECT MAX(pickup_code) FROM orders WHERE pickup_date = ?",
                String.class, Date.valueOf(pickupDate));
        if (max == null || max.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(Integer.parseInt(max));
        } catch (NumberFormatException exception) {
            return Optional.empty();
        }
    }

    /** 订单头与明细协同写入；明细先删后插，保证与传入的明细列表完全一致。 */
    public Order save(Order order) {
        int updated = jdbcTemplate.update(
                "UPDATE orders SET store_name=?, customer_name=?, status=?, created_at=?, "
                        + "estimated_ready_at=?, note=?, phone_last4=?, pickup_code=?, pickup_date=?, "
                        + "total_amount=? WHERE id=?",
                order.getStoreName(), order.getCustomerName(), order.getStatus().name(),
                Timestamp.valueOf(order.getCreatedAt()),
                Timestamp.valueOf(order.getEstimatedReadyAt()),
                order.getNote(), order.getPhoneLast4(), order.getPickupCode(),
                toSqlDate(order.getPickupDate()), order.getTotalAmount(), order.getId());
        if (updated == 0) {
            jdbcTemplate.update(
                    "INSERT INTO orders (id, store_name, customer_name, status, created_at, "
                            + "estimated_ready_at, note, phone_last4, pickup_code, pickup_date, total_amount) "
                            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    order.getId(), order.getStoreName(), order.getCustomerName(),
                    order.getStatus().name(), Timestamp.valueOf(order.getCreatedAt()),
                    Timestamp.valueOf(order.getEstimatedReadyAt()), order.getNote(),
                    order.getPhoneLast4(), order.getPickupCode(),
                    toSqlDate(order.getPickupDate()), order.getTotalAmount());
        }
        jdbcTemplate.update("DELETE FROM order_items WHERE order_id = ?", order.getId());
        for (OrderItem item : order.getOrderItems()) {
            jdbcTemplate.update(
                    "INSERT INTO order_items (order_id, line_no, product_code, drink_name, size, "
                            + "quantity, unit_price, subtotal) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                    order.getId(), item.getLineNo(), item.getProductCode(), item.getDrinkName(),
                    item.getSize(), item.getQuantity(), item.getUnitPrice(), item.getSubtotal());
        }
        return order;
    }

    /** PATCH /status 只更新订单头状态，不触碰明细。 */
    public void updateStatus(String orderId, OrderStatus status) {
        jdbcTemplate.update("UPDATE orders SET status = ? WHERE id = ?", status.name(), orderId);
    }

    private List<Order> withItems(List<Order> orders) {
        if (orders.isEmpty()) {
            return orders;
        }
        Map<String, List<OrderItem>> itemsByOrder = new HashMap<>();
        List<String> ids = new ArrayList<>();
        for (Order order : orders) {
            ids.add(order.getId());
        }
        String placeholders = String.join(",", Collections.nCopies(ids.size(), "?"));
        jdbcTemplate.query(SELECT_ITEMS_PREFIX + placeholders + ") ORDER BY order_id, line_no",
                resultSet -> {
                    String orderId = resultSet.getString("order_id");
                    itemsByOrder.computeIfAbsent(orderId, key -> new ArrayList<>())
                            .add(new OrderItem(
                                    resultSet.getInt("line_no"),
                                    resultSet.getString("product_code"),
                                    resultSet.getString("drink_name"),
                                    resultSet.getString("size"),
                                    resultSet.getInt("quantity"),
                                    resultSet.getBigDecimal("unit_price"),
                                    resultSet.getBigDecimal("subtotal")));
                },
                ids.toArray());

        List<Order> result = new ArrayList<>(orders.size());
        for (Order order : orders) {
            result.add(new Order(order.getId(), order.getStoreName(), order.getCustomerName(),
                    order.getStatus(), order.getCreatedAt(), order.getEstimatedReadyAt(),
                    order.getNote(), order.getPhoneLast4(), order.getPickupCode(),
                    order.getPickupDate(), order.getTotalAmount(),
                    itemsByOrder.getOrDefault(order.getId(), List.of())));
        }
        return result;
    }

    private Order mapHead(ResultSet resultSet, int rowNumber) throws SQLException {
        return new Order(
                resultSet.getString("id"),
                resultSet.getString("store_name"),
                resultSet.getString("customer_name"),
                OrderStatus.valueOf(resultSet.getString("status")),
                resultSet.getTimestamp("created_at").toLocalDateTime(),
                resultSet.getTimestamp("estimated_ready_at").toLocalDateTime(),
                resultSet.getString("note"),
                resultSet.getString("phone_last4"),
                resultSet.getString("pickup_code"),
                toLocalDate(resultSet.getDate("pickup_date")),
                resultSet.getBigDecimal("total_amount"),
                List.of());
    }

    private static Date toSqlDate(LocalDate date) {
        return date == null ? null : Date.valueOf(date);
    }

    private static LocalDate toLocalDate(Date date) {
        return date == null ? null : date.toLocalDate();
    }
}
