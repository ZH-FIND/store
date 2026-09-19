package com.coffeeflow.backend;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CoffeeFlowApiTests {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void healthCheckReportsUp() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.database").value("UP"));
    }

    @Test
    void listsAllV0Orders() throws Exception {
        mockMvc.perform(get("/api/v1/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(12))
                .andExpect(jsonPath("$.orders.length()").value(12))
                .andExpect(jsonPath("$.orders[0].id").value("CF-1001"))
                .andExpect(jsonPath("$.orders[0].productCode").value("CF-BEV-001"))
                .andExpect(jsonPath("$.orders[0].drinkName").value("燕麦拿铁"));
    }

    @Test
    void filtersOrdersByStatus() throws Exception {
        mockMvc.perform(get("/api/v1/orders").param("status", "READY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(3))
                .andExpect(jsonPath("$.orders.length()").value(3))
                .andExpect(jsonPath("$.orders[*].status")
                        .value(org.hamcrest.Matchers.everyItem(
                                org.hamcrest.Matchers.is("READY"))));
    }

    @Test
    void returnsOrderDetail() throws Exception {
        mockMvc.perform(get("/api/v1/orders/CF-1001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("CF-1001"))
                .andExpect(jsonPath("$.customerName").value("Alice"))
                .andExpect(jsonPath("$.productCode").value("CF-BEV-001"))
                .andExpect(jsonPath("$.size").value("大杯"))
                .andExpect(jsonPath("$.items[0]").value("燕麦拿铁"));
    }

    @Test
    void returns404ForMissingOrder() throws Exception {
        mockMvc.perform(get("/api/v1/orders/CF-9999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Order not found: CF-9999"));
    }

    @Test
    void updatesOrderStatus() throws Exception {
        mockMvc.perform(patch("/api/v1/orders/CF-1001/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"READY\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("CF-1001"))
                .andExpect(jsonPath("$.status").value("READY"));
        mockMvc.perform(get("/api/v1/orders/CF-1001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("READY"));
    }

    @Test
    void rejectsEmptyStatusWith400() throws Exception {
        mockMvc.perform(patch("/api/v1/orders/CF-1001/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Order status must not be null"));
    }

}
