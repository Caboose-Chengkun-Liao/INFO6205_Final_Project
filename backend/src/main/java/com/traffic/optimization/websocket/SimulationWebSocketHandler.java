package com.traffic.optimization.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.traffic.optimization.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * WebSocket处理器 - 定期推送仿真数据到前端
 *
 * @author Chengkun Liao, Mingjie Shen
 */
@Component
@EnableScheduling
public class SimulationWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(SimulationWebSocketHandler.class);

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
     * 定期推送仿真状态（每秒）
     */
    @Scheduled(fixedRate = 1000)
    public void broadcastSimulationState() {
        if (simulationEngine.getState() != SimulationEngine.SimulationState.RUNNING) {
            return;
        }

        try {
            // 执行仿真步进
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
            log.error("WebSocket广播错误: {}", e.getMessage());
        }
    }

    /**
     * 推送性能指标（每5秒）
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
            log.error("指标推送错误: {}", e.getMessage());
        }
    }
}
