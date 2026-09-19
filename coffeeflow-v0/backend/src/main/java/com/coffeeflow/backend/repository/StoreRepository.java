package com.coffeeflow.backend.repository;

import com.coffeeflow.backend.model.Store;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class StoreRepository {
    private static final String SELECT_COLUMNS = "SELECT store_id, store_name FROM stores";

    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<Store> rowMapper = (resultSet, rowNumber) -> new Store(
            resultSet.getString("store_id"),
            resultSet.getString("store_name"));

    public StoreRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Store> findAll() {
        return jdbcTemplate.query(SELECT_COLUMNS + " ORDER BY store_id", rowMapper);
    }

    public Optional<Store> findById(String storeId) {
        return jdbcTemplate.query(SELECT_COLUMNS + " WHERE store_id = ?", rowMapper, storeId)
                .stream().findFirst();
    }
}
