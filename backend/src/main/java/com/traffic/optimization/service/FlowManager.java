package com.traffic.optimization.service;

import com.traffic.optimization.algorithm.DijkstraAlgorithm;
import com.traffic.optimization.model.*;
import lombok.Getter;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Traffic flow manager - responsible for creating, managing and updating all traffic flows
 *
 * @author Chengkun Liao, Mingjie Shen
 */
@Service
@Getter
public class FlowManager {

    /**
     * Road network graph
     */
    private Graph graph;

    /**
     * All active traffic flows
     */
    private Map<String, TrafficFlow> activeFlows;

    /**
     * Completed traffic flows
     */
    private Map<String, TrafficFlow> completedFlows;

    /**
     * Flow ID generator
     */
    private AtomicInteger flowIdGenerator;

    /**
     * Constructor
     */
    public FlowManager() {
        this.activeFlows = new ConcurrentHashMap<>();
        this.completedFlows = new ConcurrentHashMap<>();
        this.flowIdGenerator = new AtomicInteger(1);
    }

    /**
     * Set road network graph
     */
    public void setGraph(Graph graph) {
        this.graph = graph;
    }

    /**
     * Create new traffic flow
     *
     * @param entryPoint entry node ID
     * @param destination destination node ID
     * @param numberOfCars number of vehicles
     * @return created traffic flow
     */
    public TrafficFlow createFlow(String entryPoint, String destination, int numberOfCars) {
        Node entryNode = graph.getNode(entryPoint);
        Node destNode = graph.getNode(destination);

        if (entryNode == null || destNode == null) {
            throw new IllegalArgumentException("Invalid entry or destination node");
        }

        // Generate flow ID
        String flowId = "FLOW-" + flowIdGenerator.getAndIncrement();

        // Create traffic flow
        TrafficFlow flow = new TrafficFlow(flowId, entryNode, destNode, numberOfCars);

        // Calculate shortest path
        List<Node> path = DijkstraAlgorithm.findShortestPath(graph, entryNode, destNode);

        if (path == null || path.isEmpty()) {
            throw new IllegalArgumentException("Cannot find path from " + entryPoint + " to " + destination);
        }

        flow.setPath(path);
        flow.setState(TrafficFlow.FlowState.WAITING);

        // Add to active flows list
        activeFlows.put(flowId, flow);

        return flow;
    }

    /**
     * Create multiple traffic flows in batch
     */
    public List<TrafficFlow> createMultipleFlows(List<FlowRequest> requests) {
        List<TrafficFlow> flows = new ArrayList<>();

        for (FlowRequest request : requests) {
            try {
                TrafficFlow flow = createFlow(
                    request.getEntryPoint(),
                    request.getDestination(),
                    request.getNumberOfCars()
                );
                flows.add(flow);
            } catch (Exception e) {
                System.err.println("Failed to create traffic flow: " + e.getMessage());
            }
        }

        return flows;
    }

    /**
     * Update all active traffic flows (called each time step)
     *
     * @param deltaTime time increment (seconds)
     */
    public void updateFlows(double deltaTime) {
        List<String> toRemove = new ArrayList<>();

        for (TrafficFlow flow : activeFlows.values()) {
            // Update travel time
            flow.updateTravelTime(deltaTime);

            // Check if completed
            if (flow.isCompleted()) {
                System.out.println("DEBUG FlowManager: detected completed flow " + flow.getFlowId() +
                    " state=" + flow.getState() + " isCompleted=" + flow.isCompleted());
                toRemove.add(flow.getFlowId());
                completedFlows.put(flow.getFlowId(), flow);
            }
        }

        // Remove completed flows
        for (String flowId : toRemove) {
            System.out.println("DEBUG FlowManager: removing from activeFlows " + flowId);
            activeFlows.remove(flowId);
        }

        if (toRemove.size() > 0) {
            System.out.println("DEBUG FlowManager: current activeFlows=" + activeFlows.size() +
                " completedFlows=" + completedFlows.size());
        }
    }

    /**
     * Get number of waiting flows at a specified node
     */
    public int getWaitingFlowsAtNode(Node node) {
        int count = 0;
        for (TrafficFlow flow : activeFlows.values()) {
            if (flow.getCurrentNode() != null &&
                flow.getCurrentNode().equals(node) &&
                flow.getState() == TrafficFlow.FlowState.BLOCKED) {
                count++;
            }
        }
        return count;
    }

    /**
     * Get flows on a specified edge
     */
    public List<TrafficFlow> getFlowsOnEdge(Edge edge) {
        List<TrafficFlow> flows = new ArrayList<>();
        for (TrafficFlow flow : activeFlows.values()) {
            if (flow.getCurrentEdge() != null &&
                flow.getCurrentEdge().equals(edge)) {
                flows.add(flow);
            }
        }
        return flows;
    }

    /**
     * Get list of all active flows
     */
    public List<TrafficFlow> getActiveFlowsList() {
        return new ArrayList<>(activeFlows.values());
    }

    /**
     * Get list of all completed flows
     */
    public List<TrafficFlow> getCompletedFlowsList() {
        return new ArrayList<>(completedFlows.values());
    }

    /**
     * Get total flow count
     */
    public int getTotalFlowCount() {
        return activeFlows.size() + completedFlows.size();
    }

    /**
     * Clear all flows
     */
    public void clearAllFlows() {
        activeFlows.clear();
        completedFlows.clear();
        flowIdGenerator.set(1);
    }

    /**
     * Print statistics
     */
    public void printStatistics() {
        System.out.println("=== Traffic Flow Statistics ===");
        System.out.println("Active flows: " + activeFlows.size());
        System.out.println("Completed flows: " + completedFlows.size());
        System.out.println("Total flows: " + getTotalFlowCount());
        System.out.println("==================");
    }

    /**
     * Traffic flow request class
     */
    @Getter
    public static class FlowRequest {
        private String entryPoint;
        private String destination;
        private int numberOfCars;

        public FlowRequest(String entryPoint, String destination, int numberOfCars) {
            this.entryPoint = entryPoint;
            this.destination = destination;
            this.numberOfCars = numberOfCars;
        }
    }
}
