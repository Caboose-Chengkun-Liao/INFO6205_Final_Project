package com.traffic.optimization.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * SimulationController API 集成测试
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SimulationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @Order(1)
    void testInitialize() throws Exception {
        mockMvc.perform(post("/api/simulation/initialize"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.graph.nodeCount").exists())
            .andExpect(jsonPath("$.graph.edgeCount").exists());
    }

    @Test
    @Order(2)
    void testStart() throws Exception {
        // 先初始化
        mockMvc.perform(post("/api/simulation/initialize"));

        mockMvc.perform(post("/api/simulation/start"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @Order(3)
    void testPause() throws Exception {
        mockMvc.perform(post("/api/simulation/initialize"));
        mockMvc.perform(post("/api/simulation/start"));

        mockMvc.perform(post("/api/simulation/pause"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @Order(4)
    void testGetStatus() throws Exception {
        mockMvc.perform(post("/api/simulation/initialize"));

        mockMvc.perform(get("/api/simulation/status"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.state").exists())
            .andExpect(jsonPath("$.currentTime").exists());
    }

    @Test
    @Order(5)
    void testGetGraph() throws Exception {
        mockMvc.perform(post("/api/simulation/initialize"));

        mockMvc.perform(get("/api/simulation/graph"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.nodes").isArray())
            .andExpect(jsonPath("$.edges").isArray());
    }

    @Test
    @Order(6)
    void testCreateFlow_valid() throws Exception {
        mockMvc.perform(post("/api/simulation/initialize"));

        mockMvc.perform(post("/api/simulation/flows")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"entryPoint\":\"N1\",\"destination\":\"S1\",\"numberOfCars\":10}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.flowId").exists());
    }

    @Test
    @Order(7)
    void testCreateFlow_invalidEmpty() throws Exception {
        mockMvc.perform(post("/api/simulation/initialize"));

        mockMvc.perform(post("/api/simulation/flows")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"entryPoint\":\"\",\"destination\":\"S1\",\"numberOfCars\":10}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @Order(8)
    void testCreateFlow_sameEntryDest() throws Exception {
        mockMvc.perform(post("/api/simulation/initialize"));

        mockMvc.perform(post("/api/simulation/flows")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"entryPoint\":\"N1\",\"destination\":\"N1\",\"numberOfCars\":10}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("入口和目的地不能相同"));
    }

    @Test
    @Order(9)
    void testCreateFlow_zeroCars() throws Exception {
        mockMvc.perform(post("/api/simulation/initialize"));

        mockMvc.perform(post("/api/simulation/flows")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"entryPoint\":\"N1\",\"destination\":\"S1\",\"numberOfCars\":0}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("车辆数量必须大于0"));
    }

    @Test
    @Order(10)
    void testCreateFlow_tooManyCars() throws Exception {
        mockMvc.perform(post("/api/simulation/initialize"));

        mockMvc.perform(post("/api/simulation/flows")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"entryPoint\":\"N1\",\"destination\":\"S1\",\"numberOfCars\":999}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("单次车辆数量不能超过200"));
    }

    @Test
    @Order(11)
    void testGetMetrics() throws Exception {
        mockMvc.perform(post("/api/simulation/initialize"));

        mockMvc.perform(get("/api/simulation/metrics"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.efficiency").exists());
    }

    @Test
    @Order(12)
    void testGetVehicles() throws Exception {
        mockMvc.perform(post("/api/simulation/initialize"));

        mockMvc.perform(get("/api/simulation/vehicles"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());
    }

    @Test
    @Order(13)
    void testReset() throws Exception {
        mockMvc.perform(post("/api/simulation/initialize"));
        mockMvc.perform(post("/api/simulation/start"));

        mockMvc.perform(post("/api/simulation/reset"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @Order(14)
    void testSetSignalMode() throws Exception {
        mockMvc.perform(post("/api/simulation/initialize"));

        mockMvc.perform(post("/api/simulation/signals/mode")
                .param("mode", "TRAFFIC_ADAPTIVE"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }
}
