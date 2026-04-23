package com.traffic.optimization.service;

import com.traffic.optimization.algorithm.DijkstraAlgorithm;
import com.traffic.optimization.model.*;
import lombok.Getter;
import org.springframework.stereotype.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Traffic flow manager - responsible for creating, managing, and updating all traffic flows
 *
 * @author Chengkun Liao, Mingjie Shen
 */
@Service
@Getter
public class FlowManager {

    private static final Logger log = LoggerFactory.getLogger(FlowManager.class);

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
     * Set the road network graph
     */
    public void setGraph(Graph graph) {
        this.graph = graph;
    }

    /**
     * Create a new traffic flow
     *
     * @param entryPoint   entry node ID
     * @param destination  destination node ID
     * @param numberOfCars number of vehicles
     * @return the created traffic flow
     */
    public TrafficFlow createFlow(String entryPoint, String destination, int numberOfCars) {
        Node entryNode = graph.getNode(entryPoint);
        Node destNode = graph.getNode(destination);

        if (entryNode == null || destNode == null) {
            throw new IllegalArgumentException("Invalid entry or destination node");
        }

        // Generate a flow ID
        String flowId = "FLOW-" + flowIdGenerator.getAndIncrement();

        // Create the traffic flow
        TrafficFlow flow = new TrafficFlow(flowId, entryNode, destNode, numberOfCars);

        // Compute the shortest path
        List<Node> path = DijkstraAlgorithm.findShortestPath(graph, entryNode, destNode);

        if (path == null || path.isEmpty()) {
            throw new IllegalArgumentException("No path found from " + entryPoint + " to " + destination);
        }

        flow.setPath(path);
        flow.setState(TrafficFlow.FlowState.WAITING);

        // Add to active flows
        activeFlows.put(flowId, flow);

        return flow;
    }

    /**
     * Create multiple traffic flows in bulk
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
                log.warn("Failed to create traffic flow: {}", e.getMessage());
            }
        }

        return flows;
    }

    /**
     * Update all active traffic flows (called every time step)
     *
     * @param deltaTime time increment (seconds)
     */
    public void updateFlows(double deltaTime) {
        List<String> toRemove = new ArrayList<>();

        for (TrafficFlow flow : activeFlows.values()) {
            // Update travel time
            flow.updateTravelTime(deltaTime);

            // Check whether the flow has completed
            if (flow.isCompleted()) {
                log.debug("Completed flow detected: {}", flow.getFlowId());
                toRemove.add(flow.getFlowId());
                completedFlows.put(flow.getFlowId(), flow);
            }
        }

        // Remove completed flows
        for (String flowId : toRemove) {
            activeFlows.remove(flowId);
        }

        if (!toRemove.isEmpty()) {
            log.debug("Removed {} completed flow(s); active={}, completed={}",
                toRemove.size(), activeFlows.size(), completedFlows.size());
        }
    }

    /**
     * Get the number of waiting (blocked) flows at a given node
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
     * Get all flows currently on a given edge
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
     * Get a list of all active flows
     */
    public List<TrafficFlow> getActiveFlowsList() {
        return new ArrayList<>(activeFlows.values());
    }

    /**
     * Get a list of all completed flows
     */
    public List<TrafficFlow> getCompletedFlowsList() {
        return new ArrayList<>(completedFlows.values());
    }

    /**
     * Get the total number of flows
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
        log.info("Traffic flow statistics - active: {}, completed: {}, total: {}",
            activeFlows.size(), completedFlows.size(), getTotalFlowCount());
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
