package com.coffeeflow.backend.repository;

import com.coffeeflow.backend.model.Order;
import com.coffeeflow.backend.model.OrderStatus;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class OrderRepository {
    private static final String SELECT_COLUMNS =
            "SELECT id, store_name, customer_name, product_code, drink_name, size, quantity, status, "
                    + "created_at, estimated_ready_at, items, note FROM orders";

    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<Order> rowMapper = this::mapOrder;

    public OrderRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public long count() {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM orders", Long.class);
        return count == null ? 0 : count;
    }

    public List<Order> findAll() {
        return jdbcTemplate.query(SELECT_COLUMNS + " ORDER BY id", rowMapper);
    }

    public List<Order> findAllByStatus(OrderStatus status) {
        return jdbcTemplate.query(SELECT_COLUMNS + " WHERE status = ? ORDER BY id",
                rowMapper, status.name());
    }

    public Optional<Order> findById(String id) {
        return jdbcTemplate.query(SELECT_COLUMNS + " WHERE id = ?", rowMapper, id)
                .stream().findFirst();
    }

    public Order save(Order order) {
        int updated = jdbcTemplate.update(
                "UPDATE orders SET store_name=?, customer_name=?, product_code=?, drink_name=?, size=?, "
                        + "quantity=?, status=?, created_at=?, estimated_ready_at=?, items=?, note=? "
                        + "WHERE id=?",
                order.getStoreName(), order.getCustomerName(), order.getProductCode(),
                order.getDrinkName(), order.getSize(),
                order.getQuantity(), order.getStatus().name(), Timestamp.valueOf(order.getCreatedAt()),
                Timestamp.valueOf(order.getEstimatedReadyAt()), String.join("|", order.getItems()),
                order.getNote(), order.getId());
        if (updated == 0) {
            jdbcTemplate.update(
                    "INSERT INTO orders (id, store_name, customer_name, product_code, drink_name, size, quantity, "
                            + "status, created_at, estimated_ready_at, items, note) "
                            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    order.getId(), order.getStoreName(), order.getCustomerName(), order.getProductCode(),
                    order.getDrinkName(),
                    order.getSize(), order.getQuantity(), order.getStatus().name(),
                    Timestamp.valueOf(order.getCreatedAt()),
                    Timestamp.valueOf(order.getEstimatedReadyAt()),
                    String.join("|", order.getItems()), order.getNote());
        }
        return order;
    }

    public void saveAll(List<Order> orders) {
        orders.forEach(this::save);
    }

    private Order mapOrder(ResultSet resultSet, int rowNumber) throws SQLException {
        String rawItems = resultSet.getString("items");
        List<String> items = rawItems == null || rawItems.isBlank()
                ? List.of() : Arrays.asList(rawItems.split("\\|", -1));
        return new Order(
                resultSet.getString("id"),
                resultSet.getString("store_name"),
                resultSet.getString("customer_name"),
                resultSet.getString("product_code"),
                resultSet.getString("drink_name"),
                resultSet.getString("size"),
                resultSet.getInt("quantity"),
                OrderStatus.valueOf(resultSet.getString("status")),
                resultSet.getTimestamp("created_at").toLocalDateTime(),
                resultSet.getTimestamp("estimated_ready_at").toLocalDateTime(),
                items,
                resultSet.getString("note"));
    }
}
