package com.coffeeflow.backend.repository;

import com.coffeeflow.backend.model.Product;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class ProductRepository {
    private static final String CATALOG_COLUMNS = "SELECT product_code, name, price FROM products";

    private static final String STORE_PRODUCT_COLUMNS =
            "SELECT p.product_code, p.name, p.price, sp.available FROM store_product sp "
                    + "JOIN products p ON p.product_code = sp.product_code";

    private static final RowMapper<Product> CATALOG_MAPPER = (resultSet, rowNumber) -> new Product(
            resultSet.getString("product_code"),
            resultSet.getString("name"),
            resultSet.getBigDecimal("price"));

    private static final RowMapper<Product> STORE_MAPPER = (resultSet, rowNumber) -> new Product(
            resultSet.getString("product_code"),
            resultSet.getString("name"),
            resultSet.getBigDecimal("price"),
            resultSet.getBoolean("available"));

    private final JdbcTemplate jdbcTemplate;

    public ProductRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Product> findAll() {
        return jdbcTemplate.query(CATALOG_COLUMNS + " ORDER BY product_code", CATALOG_MAPPER);
    }

    public Optional<Product> findById(String productCode) {
        return jdbcTemplate.query(CATALOG_COLUMNS + " WHERE product_code = ?", CATALOG_MAPPER, productCode)
                .stream().findFirst();
    }

    /** 门店视角的商品列表：停售商品仍返回，只是 available = false。 */
    public List<Product> findByStoreId(String storeId) {
        return jdbcTemplate.query(STORE_PRODUCT_COLUMNS + " WHERE sp.store_id = ? ORDER BY p.product_code",
                STORE_MAPPER, storeId);
    }

    /** 门店视角的单个商品；商品不属于该门店时返回空。 */
    public Optional<Product> findStoreProduct(String storeId, String productCode) {
        return jdbcTemplate.query(
                STORE_PRODUCT_COLUMNS + " WHERE sp.store_id = ? AND sp.product_code = ?",
                STORE_MAPPER, storeId, productCode)
                .stream().findFirst();
    }

    public void updateAvailability(String storeId, String productCode, boolean available) {
        jdbcTemplate.update(
                "UPDATE store_product SET available = ? WHERE store_id = ? AND product_code = ?",
                available, storeId, productCode);
    }
}
