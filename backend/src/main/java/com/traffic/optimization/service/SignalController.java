package com.traffic.optimization.service;

import com.traffic.optimization.model.Graph;
import com.traffic.optimization.model.Node;
import com.traffic.optimization.model.NodeType;
import com.traffic.optimization.model.TrafficLight;
import lombok.Getter;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Signal controller - responsible for managing traffic lights at all intersections
 *
 * @author Chengkun Liao, Mingjie Shen
 */
@Service
@Getter
public class SignalController {

    /**
     * Road network graph
     */
    private Graph graph;

    /**
     * Flow manager
     */
    private FlowManager flowManager;

    /**
     * Optimization mode
     */
    private OptimizationMode mode;

    /**
     * Optimization history records
     */
    private List<OptimizationRecord> optimizationHistory;

    /**
     * Constructor
     */
    public SignalController() {
        this.mode = OptimizationMode.FIXED_TIME;
        this.optimizationHistory = new ArrayList<>();
    }

    /**
     * Set road network graph
     */
    public void setGraph(Graph graph) {
        this.graph = graph;
    }

    /**
     * Set flow manager
     */
    public void setFlowManager(FlowManager flowManager) {
        this.flowManager = flowManager;
    }

    /**
     * Set optimization mode
     */
    public void setOptimizationMode(OptimizationMode mode) {
        this.mode = mode;
    }

    /**
     * Update all traffic lights (called each second)
     */
    public void updateSignals() {
        if (graph == null) {
            return;
        }

        for (Node node : graph.getIntersectionNodes()) {
            if (node.getTrafficLight() != null) {
                node.getTrafficLight().update();
            }
        }
    }

    /**
     * Optimize signal timing
     */
    public void optimizeSignals() {
        if (graph == null || flowManager == null) {
            return;
        }

        switch (mode) {
            case FIXED_TIME:
                // Fixed time mode, no optimization
                break;

            case TRAFFIC_ADAPTIVE:
                optimizeByTrafficVolume();
                break;

            case LEARNING_BASED:
                optimizeByLearning();
                break;
        }
    }

    /**
     * Traffic volume-based adaptive optimization
     */
    private void optimizeByTrafficVolume() {
        for (Node node : graph.getIntersectionNodes()) {
            TrafficLight light = node.getTrafficLight();
            if (light == null) {
                continue;
            }

            // Calculate waiting vehicles at this intersection
            int waitingVehicles = flowManager.getWaitingFlowsAtNode(node);

            // Adjust green light duration based on waiting vehicles
            int newGreenDuration = calculateOptimalGreenTime(waitingVehicles);

            // Apply new green light duration
            light.adjustGreenDuration(newGreenDuration);
        }
    }

    /**
     * Calculate optimal green light time
     */
    private int calculateOptimalGreenTime(int waitingVehicles) {
        // Base duration
        int baseTime = 20;

        // Increase duration based on waiting vehicles (5 seconds per 10 vehicles)
        int additionalTime = (waitingVehicles / 10) * 5;

        // Total duration limited to 15-60 seconds
        return Math.max(15, Math.min(60, baseTime + additionalTime));
    }

    /**
     * Learning-based optimization (simplified version)
     */
    private void optimizeByLearning() {
        // TODO: Implement learning optimization based on historical data
        // Can use reinforcement learning or other machine learning algorithms

        // Current simplified implementation: use historical average efficiency to adjust
        optimizeByTrafficVolume();
    }

    /**
     * Manually set signal timing for a single intersection
     */
    public void setSignalTiming(String nodeId, int greenDuration) {
        Node node = graph.getNode(nodeId);
        if (node != null && node.getType() == NodeType.INTERSECTION) {
            TrafficLight light = node.getTrafficLight();
            if (light != null) {
                light.adjustGreenDuration(greenDuration);
            }
        }
    }

    /**
     * Synchronize all traffic lights (to coordinate their operation)
     */
    public void synchronizeSignals() {
        List<Node> intersections = graph.getIntersectionNodes();
        if (intersections.isEmpty()) {
            return;
        }

        // Get main road intersections
        // Simplified implementation: synchronize all east-west signals
        for (Node node : intersections) {
            TrafficLight light = node.getTrafficLight();
            if (light != null) {
                // Reset to the same initial state
                light.setCurrentDirection(TrafficLight.SignalDirection.EAST_WEST);
                light.setCurrentState(TrafficLight.SignalState.GREEN);
                light.setRemainingTime(light.getGreenDuration());
            }
        }
    }

    /**
     * Get all signal statuses
     */
    public List<SignalStatus> getAllSignalStatuses() {
        List<SignalStatus> statuses = new ArrayList<>();

        for (Node node : graph.getIntersectionNodes()) {
            TrafficLight light = node.getTrafficLight();
            if (light != null) {
                SignalStatus status = new SignalStatus(
                    node.getId(),
                    node.getName(),
                    light.getCurrentDirection(),
                    light.getCurrentState(),
                    light.getRemainingTime(),
                    light.getGreenDuration()
                );
                statuses.add(status);
            }
        }

        return statuses;
    }

    /**
     * Record optimization result
     */
    public void recordOptimization(double efficiency) {
        OptimizationRecord record = new OptimizationRecord(
            System.currentTimeMillis(),
            mode,
            efficiency
        );
        optimizationHistory.add(record);

        // Keep only the most recent 100 records
        if (optimizationHistory.size() > 100) {
            optimizationHistory.remove(0);
        }
    }

    /**
     * Optimization mode enumeration
     */
    public enum OptimizationMode {
        FIXED_TIME,         // fixed time mode
        TRAFFIC_ADAPTIVE,   // traffic adaptive mode
        LEARNING_BASED      // learning-based mode
    }

    /**
     * Signal status class
     */
    @Getter
    public static class SignalStatus {
        private String nodeId;
        private String nodeName;
        private TrafficLight.SignalDirection direction;
        private TrafficLight.SignalState state;
        private int remainingTime;
        private int greenDuration;

        public SignalStatus(String nodeId, String nodeName,
                          TrafficLight.SignalDirection direction,
                          TrafficLight.SignalState state,
                          int remainingTime, int greenDuration) {
            this.nodeId = nodeId;
            this.nodeName = nodeName;
            this.direction = direction;
            this.state = state;
            this.remainingTime = remainingTime;
            this.greenDuration = greenDuration;
        }
    }

    /**
     * Optimization record class
     */
    @Getter
    public static class OptimizationRecord {
        private long timestamp;
        private OptimizationMode mode;
        private double efficiency;

        public OptimizationRecord(long timestamp, OptimizationMode mode, double efficiency) {
            this.timestamp = timestamp;
            this.mode = mode;
            this.efficiency = efficiency;
        }
    }
}
