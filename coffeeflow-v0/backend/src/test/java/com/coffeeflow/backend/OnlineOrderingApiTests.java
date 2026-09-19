package com.coffeeflow.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

/**
 * 顾客下单 / 取餐码 / 取消 / 门店停售的集成测试。
 * 全部 @Transactional，改动在用例结束后回滚，不污染 H2 基线数据。
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class OnlineOrderingApiTests {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void listsStores() throws Exception {
        mockMvc.perform(get("/api/v1/stores"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(5))
                .andExpect(jsonPath("$.stores[0].storeId").value("S001"))
                .andExpect(jsonPath("$.stores[0].storeName").value("国贸店"));
    }

    @Test
    void listsProductsOfExistingStore() throws Exception {
        mockMvc.perform(get("/api/v1/stores/S001/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(14))
                .andExpect(jsonPath("$.products[0].productCode").value("CF-BEV-001"))
                .andExpect(jsonPath("$.products[0].name").value("燕麦拿铁"))
                .andExpect(jsonPath("$.products[0].available").value(true))
                .andExpect(jsonPath("$.products[2].productCode").value("CF-BEV-003"))
                .andExpect(jsonPath("$.products[2].name").value("抹茶拿铁"));
    }

    @Test
    void returns404ForProductsOfMissingStore() throws Exception {
        mockMvc.perform(get("/api/v1/stores/S999/products"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Store not found: S999"));
    }

    @Test
    void stopSellingOnlyAffectsTheGivenStore() throws Exception {
        mockMvc.perform(patch("/api/v1/stores/S001/products/CF-BEV-001/availability")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"available\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productCode").value("CF-BEV-001"))
                .andExpect(jsonPath("$.available").value(false));

        mockMvc.perform(get("/api/v1/stores/S001/products"))
                .andExpect(jsonPath("$.products[0].available").value(false));
        mockMvc.perform(get("/api/v1/stores/S002/products"))
                .andExpect(jsonPath("$.products[0].available").value(true));

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding(StandardCharsets.UTF_8.name())
                        .content(createOrderBody("S001", "顾客A", "1234", item("CF-BEV-001", "中杯", 1))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Product is not available in store S001: CF-BEV-001"));

        // B 店未停售，仍可正常下单
        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding(StandardCharsets.UTF_8.name())
                        .content(createOrderBody("S002", "顾客B", "5678", item("CF-BEV-001", "中杯", 1))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("NEW"));
    }

    @Test
    void resumesStoppedProduct() throws Exception {
        mockMvc.perform(patch("/api/v1/stores/S001/products/CF-BEV-002/availability")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"available\":false}"))
                .andExpect(status().isOk());
        mockMvc.perform(patch("/api/v1/stores/S001/products/CF-BEV-002/availability")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"available\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(true));

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding(StandardCharsets.UTF_8.name())
                        .content(createOrderBody("S001", "顾客A", "1234", item("CF-BEV-002", "小杯", 1))))
                .andExpect(status().isCreated());
    }

    @Test
    void returns404WhenStoppingProductOfMissingStore() throws Exception {
        mockMvc.perform(patch("/api/v1/stores/S999/products/CF-BEV-001/availability")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"available\":false}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Store not found: S999"));

        mockMvc.perform(patch("/api/v1/stores/S001/products/CF-BEV-999/availability")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"available\":false}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Product not found in store S001: CF-BEV-999"));
    }

    @Test
    void createsOrderAndExposesItOnTheDashboard() throws Exception {
        MvcResult created = createOrder("S002", "顾客C", "4321",
                item("CF-BEV-002", "小杯", 2) + "," + item("CF-BEV-004", "大杯", 1));

        JsonNode body = toJson(created);
        String orderId = body.get("orderId").asText();
        // 订单号沿用 CF- 前缀递增，不与历史 12 条冲突
        assertThat(orderId).isEqualTo("CF-1013");
        assertThat(body.get("pickupCode").asText()).isEqualTo("1001");
        assertThat(body.get("status").asText()).isEqualTo("NEW");
        assertThat(body.get("totalAmount").decimalValue()).isEqualByComparingTo(new BigDecimal("72.00"));

        mockMvc.perform(get("/api/v1/orders/" + orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.storeName").value("望京店"))
                .andExpect(jsonPath("$.quantity").value(2))
                .andExpect(jsonPath("$.productCode").value("CF-BEV-002"))
                .andExpect(jsonPath("$.drinkName").value("美式"))
                .andExpect(jsonPath("$.size").value("小杯"))
                .andExpect(jsonPath("$.items[0]").value("美式"))
                .andExpect(jsonPath("$.items[1]").value("冷萃"))
                .andExpect(jsonPath("$.status").value("NEW"));

        // 看板按门店名可筛到该订单
        mockMvc.perform(get("/api/v1/orders").param("status", "NEW"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orders[?(@.id == '" + orderId + "')].storeName",
                        org.hamcrest.Matchers.contains("望京店")));
    }

    @Test
    void rejectsOrderWithUnavailableProductAndCreatesNothing() throws Exception {
        jdbcTemplate.update(
                "UPDATE store_product SET available = FALSE WHERE store_id = ? AND product_code = ?",
                "S003", "CF-BEV-005");

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding(StandardCharsets.UTF_8.name())
                        .content(createOrderBody("S003", "顾客C", "4321", item("CF-BEV-005", "中杯", 1))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Product is not available in store S003: CF-BEV-005"));

        assertThat(orderCount()).isEqualTo(12);
    }

    @Test
    void rejectsOrderWithInvalidQuantityAndUnknownProduct() throws Exception {
        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding(StandardCharsets.UTF_8.name())
                        .content(createOrderBody("S001", "顾客C", "4321", item("CF-BEV-002", "中杯", 0))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Quantity must be at least 1 for product: CF-BEV-002"));

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding(StandardCharsets.UTF_8.name())
                        .content(createOrderBody("S001", "顾客C", "4321", item("CF-BEV-999", "中杯", 1))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Product not found: CF-BEV-999"));

        assertThat(orderCount()).isEqualTo(12);
    }

    @Test
    void rejectsOrderWithMissingRequiredFields() throws Exception {
        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding(StandardCharsets.UTF_8.name())
                        .content(createOrderBody("S999", "顾客C", "4321", item("CF-BEV-002", "中杯", 1))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Store not found: S999"));

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding(StandardCharsets.UTF_8.name())
                        .content(createOrderBody("S001", "顾客C", "4321", "")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Order items must not be empty"));

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding(StandardCharsets.UTF_8.name())
                        .content(createOrderBody("S001", "顾客C", "12", item("CF-BEV-002", "中杯", 1))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Phone last 4 digits must be 4 digits"));

        assertThat(orderCount()).isEqualTo(12);
    }

    @Test
    void queriesOrderByPickupCodeOfTodayOnly() throws Exception {
        MvcResult created = createOrder("S001", "顾客D", "8888", item("CF-BEV-003", "中杯", 1));
        String orderId = toJson(created).get("orderId").asText();

        MvcResult pickupResult = mockMvc.perform(get("/api/v1/orders/pickup/1001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(orderId))
                .andExpect(jsonPath("$.pickupCode").value("1001"))
                .andExpect(jsonPath("$.storeName").value("国贸店"))
                .andExpect(jsonPath("$.status").value("NEW"))
                .andExpect(jsonPath("$.items[0].productCode").value("CF-BEV-003"))
                .andExpect(jsonPath("$.items[0].drinkName").value("抹茶拿铁"))
                .andExpect(jsonPath("$.items[0].size").value("中杯"))
                .andExpect(jsonPath("$.items[0].quantity").value(1))
                .andReturn();
        assertThat(toJson(pickupResult).get("totalAmount").decimalValue())
                .isEqualByComparingTo(new BigDecimal("30.00"));

        // 门店推进状态后，取餐码查询能看到最新状态
        mockMvc.perform(patch("/api/v1/orders/" + orderId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"IN_PROGRESS\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/orders/pickup/1001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

        mockMvc.perform(get("/api/v1/orders/pickup/9999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Order not found for pickup code: 9999"));
    }

    @Test
    void cancelsNewOrderAndKeepsItOnTheDashboard() throws Exception {
        MvcResult created = createOrder("S001", "顾客E", "2468", item("CF-BEV-002", "中杯", 1));
        String orderId = toJson(created).get("orderId").asText();

        mockMvc.perform(post("/api/v1/orders/" + orderId + "/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pickupCode\":\"1001\",\"phoneLast4\":\"2468\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.orderId").value(orderId));

        // 取消只改状态不删单，门店看板仍能看到
        mockMvc.perform(get("/api/v1/orders/" + orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"))
                .andExpect(jsonPath("$.storeName").value("国贸店"));
        assertThat(orderCount()).isEqualTo(13);
    }

    @Test
    void rejectsCancelWhenOrderIsInProgress() throws Exception {
        MvcResult created = createOrder("S001", "顾客E", "2468", item("CF-BEV-002", "中杯", 1));
        String orderId = toJson(created).get("orderId").asText();

        mockMvc.perform(patch("/api/v1/orders/" + orderId + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"IN_PROGRESS\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/orders/" + orderId + "/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pickupCode\":\"1001\",\"phoneLast4\":\"2468\"}"))
                .andExpect(status().isConflict());

        mockMvc.perform(get("/api/v1/orders/" + orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    void rejectsCancelWithMismatchedPhoneOrPickupCode() throws Exception {
        MvcResult created = createOrder("S001", "顾客E", "2468", item("CF-BEV-002", "中杯", 1));
        String orderId = toJson(created).get("orderId").asText();

        mockMvc.perform(post("/api/v1/orders/" + orderId + "/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pickupCode\":\"1001\",\"phoneLast4\":\"0000\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Phone last 4 digits do not match order: " + orderId));

        mockMvc.perform(post("/api/v1/orders/" + orderId + "/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pickupCode\":\"2002\",\"phoneLast4\":\"2468\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Pickup code does not match order: " + orderId));

        mockMvc.perform(get("/api/v1/orders/" + orderId))
                .andExpect(jsonPath("$.status").value("NEW"));
    }

    @Test
    void rejectsRepeatedCancel() throws Exception {
        MvcResult created = createOrder("S001", "顾客E", "2468", item("CF-BEV-002", "中杯", 1));
        String orderId = toJson(created).get("orderId").asText();

        String cancelBody = "{\"pickupCode\":\"1001\",\"phoneLast4\":\"2468\"}";
        mockMvc.perform(post("/api/v1/orders/" + orderId + "/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cancelBody))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/orders/" + orderId + "/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cancelBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Order already cancelled: " + orderId));
    }

    @Test
    void reusesPickupCodeOnDifferentDatesButResetsTodayFrom1001() throws Exception {
        insertHistoricalOrder("CF-9001", "1001", LocalDate.now().minusDays(1));

        MvcResult first = createOrder("S001", "顾客F", "1357", item("CF-BEV-002", "中杯", 1));
        assertThat(toJson(first).get("pickupCode").asText()).isEqualTo("1001");

        MvcResult second = createOrder("S001", "顾客F", "1357", item("CF-BEV-002", "中杯", 1));
        assertThat(toJson(second).get("pickupCode").asText()).isEqualTo("1002");

        Integer sameCode = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM orders WHERE pickup_code = ?", Integer.class, "1001");
        assertThat(sameCode).isEqualTo(2);
    }

    @Test
    void keepsOrderAmountAfterProductPriceChanges() throws Exception {
        MvcResult created = createOrder("S001", "顾客G", "1111", item("CF-BEV-002", "中杯", 2));
        String pickupCode = toJson(created).get("pickupCode").asText();
        assertThat(toJson(created).get("totalAmount").decimalValue())
                .isEqualByComparingTo(new BigDecimal("44.00"));

        jdbcTemplate.update("UPDATE products SET price = ? WHERE product_code = ?",
                new BigDecimal("999.00"), "CF-BEV-002");

        MvcResult result = mockMvc.perform(get("/api/v1/orders/pickup/" + pickupCode))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode body = toJson(result);
        assertThat(body.get("totalAmount").decimalValue()).isEqualByComparingTo(new BigDecimal("44.00"));
        assertThat(body.get("items").get(0).get("unitPrice").decimalValue())
                .isEqualByComparingTo(new BigDecimal("22.00"));
        assertThat(body.get("items").get(0).get("subtotal").decimalValue())
                .isEqualByComparingTo(new BigDecimal("44.00"));
    }

    @Test
    void keepsUnitPriceAndTotalIndependentOfSize() throws Exception {
        MvcResult small = createOrder("S001", "顾客H", "2222", item("CF-BEV-001", "小杯", 1));
        MvcResult large = createOrder("S001", "顾客H", "2222", item("CF-BEV-001", "大杯", 1));

        BigDecimal smallTotal = toJson(small).get("totalAmount").decimalValue();
        BigDecimal largeTotal = toJson(large).get("totalAmount").decimalValue();
        assertThat(smallTotal).isEqualByComparingTo(new BigDecimal("32.00"));
        assertThat(largeTotal).isEqualByComparingTo(smallTotal);
    }

    private MvcResult createOrder(String storeId, String customerName, String phoneLast4, String items)
            throws Exception {
        return mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding(StandardCharsets.UTF_8.name())
                        .content(createOrderBody(storeId, customerName, phoneLast4, items)))
                .andExpect(status().isCreated())
                .andReturn();
    }

    private String createOrderBody(String storeId, String customerName, String phoneLast4, String items) {
        return "{\"storeId\":\"" + storeId + "\",\"customerName\":\"" + customerName
                + "\",\"phoneLast4\":\"" + phoneLast4 + "\",\"note\":\"顾客备注\",\"items\":["
                + items + "]}";
    }

    private String item(String productCode, String size, int quantity) {
        return "{\"productCode\":\"" + productCode + "\",\"size\":\"" + size
                + "\",\"quantity\":" + quantity + "}";
    }

    private JsonNode toJson(MvcResult result) throws Exception {
        return OBJECT_MAPPER.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    private int orderCount() {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM orders", Integer.class);
        return count == null ? 0 : count;
    }

    /** 造一条历史日期、已占用取餐码 1001 的订单，用于验证按日重置与同日唯一。 */
    private void insertHistoricalOrder(String orderId, String pickupCode, LocalDate pickupDate) {
        jdbcTemplate.update(
                "INSERT INTO orders (id, store_name, customer_name, status, created_at, "
                        + "estimated_ready_at, note, phone_last4, pickup_code, pickup_date, total_amount) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                orderId, "国贸店", "历史顾客", "NEW",
                Timestamp.valueOf(LocalDateTime.now().minusDays(1)),
                Timestamp.valueOf(LocalDateTime.now().minusDays(1).plusMinutes(15)),
                "历史订单", null, pickupCode, Date.valueOf(pickupDate), BigDecimal.ZERO);
    }
}
