package com.traffic.optimization.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.traffic.optimization.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * WebSocket handler - periodically pushes simulation data to the frontend
 *
 * @author Chengkun Liao, Mingjie Shen
 */
@Component
@EnableScheduling
public class SimulationWebSocketHandler {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private SimulationEngine simulationEngine;

    @Autowired
    private FlowManager flowManager;

    @Autowired
    private SignalController signalController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Periodically push simulation state (every second)
     */
    @Scheduled(fixedRate = 1000)
    public void broadcastSimulationState() {
        if (simulationEngine.getState() != SimulationEngine.SimulationState.RUNNING) {
            return;
        }

        try {
            // Execute simulation step
            simulationEngine.step();

            Map<String, Object> data = new HashMap<>();
            data.put("timestamp", System.currentTimeMillis());
            data.put("currentTime", simulationEngine.getCurrentTime());
            data.put("state", simulationEngine.getState());
            data.put("metrics", simulationEngine.getCurrentMetrics());
            data.put("activeFlows", flowManager.getActiveFlowsList());
            data.put("signals", signalController.getAllSignalStatuses());

            messagingTemplate.convertAndSend("/topic/simulation", data);
        } catch (Exception e) {
            System.err.println("WebSocket broadcast error: " + e.getMessage());
        }
    }

    /**
     * Push performance metrics (every 5 seconds)
     */
    @Scheduled(fixedRate = 5000)
    public void broadcastMetrics() {
        if (simulationEngine.getState() != SimulationEngine.SimulationState.RUNNING) {
            return;
        }

        try {
            EfficiencyCalculator.PerformanceMetrics metrics = simulationEngine.getCurrentMetrics();
            messagingTemplate.convertAndSend("/topic/metrics", metrics);
        } catch (Exception e) {
            System.err.println("Metrics push error: " + e.getMessage());
        }
    }
}
