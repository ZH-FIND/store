package com.coffeeflow.backend;

import static org.assertj.core.api.Assertions.assertThat;

import com.coffeeflow.backend.model.OrderStatus;
import com.coffeeflow.backend.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
class JdbcBaselineTests {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private OrderRepository orderRepository;

    @Test
    void v0ReadsAndUpdatesTheOrdersFulfillmentTableThroughJdbcTemplate() {
        assertThat(orderRepository.count()).isEqualTo(12);
        assertThat(orderRepository.findById("CF-1001")).isPresent();

        var order = orderRepository.findById("CF-1001").orElseThrow();
        assertThat(order.getProductCode()).isEqualTo("CF-BEV-001");
        assertThat(orderRepository.findById("CF-1012").orElseThrow().getProductCode())
                .isEqualTo("CF-BEV-014");
        order.setStatus(OrderStatus.IN_PROGRESS);
        orderRepository.save(order);

        String storedStatus = jdbcTemplate.queryForObject(
                "SELECT status FROM orders WHERE id = ?", String.class, "CF-1001");
        assertThat(storedStatus).isEqualTo("IN_PROGRESS");
    }

    @Test
    void seedDataKeepsDashboardSummaryStable() {
        Integer overdue = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM orders "
                        + "WHERE status NOT IN ('COMPLETED', 'CANCELLED') "
                        + "AND estimated_ready_at < CURRENT_TIMESTAMP",
                Integer.class);
        Integer brewing = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM orders WHERE status = 'IN_PROGRESS'",
                Integer.class);
        Integer ready = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM orders WHERE status = 'READY'",
                Integer.class);

        assertThat(orderRepository.count()).isEqualTo(12);
        assertThat(overdue).isEqualTo(4);
        assertThat(brewing).isEqualTo(3);
        assertThat(ready).isEqualTo(3);
    }

    @Test
    void historicalOrdersCanContainDifferentProducts() {
        var order = orderRepository.findById("CF-1003").orElseThrow();
        assertThat(order.getItems()).containsExactly("抹茶拿铁", "美式", "冷萃");
    }
}
