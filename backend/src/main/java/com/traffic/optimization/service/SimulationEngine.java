package com.traffic.optimization.service;

import com.traffic.optimization.model.*;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Simulation engine - responsible for the time-step simulation of the entire traffic system
 *
 * @author Chengkun Liao, Mingjie Shen
 */
@Service
@Getter
public class SimulationEngine {

    @Autowired
    private FlowManager flowManager;

    @Autowired
    private SignalController signalController;

    @Autowired
    private EfficiencyCalculator efficiencyCalculator;

    /**
     * Road network graph
     */
    private Graph graph;

    /**
     * Simulation state
     */
    private SimulationState state;

    /**
     * Current simulation time (seconds)
     */
    private long currentTime;

    /**
     * Time step (seconds)
     */
    private double timeStep;

    /**
     * Simulation speed multiplier
     */
    private double speedMultiplier;

    /**
     * Efficiency evaluation interval (seconds)
     */
    private long efficiencyEvaluationInterval;

    /**
     * Last efficiency evaluation time
     */
    private long lastEfficiencyEvaluationTime;

    /**
     * Whether continuous flow generation is enabled
     */
    private boolean continuousFlowEnabled;

    /**
     * Flow generation interval (seconds)
     */
    private long flowGenerationInterval;

    /**
     * Last flow generation time
     */
    private long lastFlowGenerationTime;

    /**
     * Random number generator
     */
    private Random random;

    /**
     * Constructor
     */
    public SimulationEngine() {
        this.state = SimulationState.STOPPED;
        this.currentTime = 0;
        this.timeStep = 1.0; // default 1 second
        this.speedMultiplier = 1.0;
        this.efficiencyEvaluationInterval = 3600; // evaluate once per hour
        this.lastEfficiencyEvaluationTime = 0;
        this.continuousFlowEnabled = true; // enable continuous flow by default
        this.flowGenerationInterval = 30; // generate new flows every 30 seconds
        this.lastFlowGenerationTime = 0;
        this.random = new Random();
    }

    /**
     * Initialize simulation
     */
    public void initialize(Graph graph) {
        this.graph = graph;
        this.flowManager.setGraph(graph);
        this.signalController.setGraph(graph);
        this.signalController.setFlowManager(flowManager);

        this.currentTime = 0;
        this.state = SimulationState.INITIALIZED;

        System.out.println("Simulation engine initialized");
    }

    /**
     * Start simulation
     */
    public void start() {
        if (state == SimulationState.INITIALIZED || state == SimulationState.PAUSED) {
            state = SimulationState.RUNNING;

            // Generate initial flows immediately so users can see vehicles right away
            if (continuousFlowEnabled && state == SimulationState.RUNNING) {
                System.out.println("Generating initial traffic flows...");
                generateRandomFlows();
                generateRandomFlows(); // generate two batches of initial flows
            }

            System.out.println("Simulation started");
        }
    }

    /**
     * Pause simulation
     */
    public void pause() {
        if (state == SimulationState.RUNNING) {
            state = SimulationState.PAUSED;
            System.out.println("Simulation paused");
        }
    }

    /**
     * Stop simulation
     */
    public void stop() {
        state = SimulationState.STOPPED;
        currentTime = 0;
        System.out.println("Simulation stopped");
    }

    /**
     * Reset simulation
     */
    public void reset() {
        System.out.println("=== DEBUG: reset() called ===");
        System.out.println("Call stack:");
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        for (int i = 2; i < Math.min(stackTrace.length, 8); i++) {
            System.out.println("  " + stackTrace[i]);
        }

        stop();
        flowManager.clearAllFlows();
        efficiencyCalculator.clearHistory();
        currentTime = 0;
        lastEfficiencyEvaluationTime = 0;
        state = SimulationState.INITIALIZED;
        System.out.println("Simulation reset");
    }

    /**
     * Execute single time step (main simulation loop)
     */
    public void step() {
        if (state != SimulationState.RUNNING) {
            return;
        }

        // 1. Update traffic lights
        signalController.updateSignals();

        // 2. Update traffic flows
        updateTrafficFlows();

        // 3. Update flow manager
        flowManager.updateFlows(timeStep);

        // 4. Increment simulation time
        currentTime += (long) timeStep;

        // 5. Continuously generate new flows (if enabled)
        if (continuousFlowEnabled && currentTime - lastFlowGenerationTime >= flowGenerationInterval) {
            generateRandomFlows();
            lastFlowGenerationTime = currentTime;
        }

        // 6. Periodically evaluate efficiency
        if (currentTime - lastEfficiencyEvaluationTime >= efficiencyEvaluationInterval) {
            evaluateEfficiency();
            lastEfficiencyEvaluationTime = currentTime;
        }

        // 7. Periodically optimize traffic lights
        if (currentTime % 300 == 0) { // optimize every 5 minutes
            signalController.optimizeSignals();
        }
    }

    /**
     * Update traffic flow movement
     */
    private void updateTrafficFlows() {
        List<TrafficFlow> activeFlows = flowManager.getActiveFlowsList();

        for (TrafficFlow flow : activeFlows) {
            // Skip completed flows
            if (flow.getState() == TrafficFlow.FlowState.COMPLETED) {
                System.out.println("DEBUG updateTrafficFlows: skipping completed flow " + flow.getFlowId());
                continue;
            }

            if (flow.getState() == TrafficFlow.FlowState.WAITING) {
                // Try to enter the network
                tryEnterNetwork(flow);
            } else if (flow.getState() == TrafficFlow.FlowState.ACTIVE) {
                // Try to move to the next node
                tryMoveToNextNode(flow);
            } else if (flow.getState() == TrafficFlow.FlowState.BLOCKED) {
                // Blocked vehicles still need to keep trying to move (signal may have turned green)
                tryMoveToNextNode(flow);
            }
        }
    }

    /**
     * Try to enter the network for a flow
     */
    private void tryEnterNetwork(TrafficFlow flow) {
        Node currentNode = flow.getCurrentNode();
        Node nextNode = flow.getNextNode();

        System.out.println("DEBUG tryEnterNetwork: " + flow.getFlowId() +
            " currentNode=" + (currentNode != null ? currentNode.getId() : "null") +
            " nextNode=" + (nextNode != null ? nextNode.getId() : "null"));

        if (nextNode == null) {
            System.out.println("DEBUG: nextNode is null for " + flow.getFlowId());
            return;
        }

        Edge edge = currentNode.getEdgeTo(nextNode);
        System.out.println("DEBUG: edge=" + (edge != null ? edge.getId() : "null") +
            " isFull=" + (edge != null ? edge.isFull() : "N/A"));

        if (edge != null && !edge.isFull()) {
            // Check traffic light (if current node is an intersection)
            boolean canPass = canPass(currentNode, nextNode);
            System.out.println("DEBUG: canPass=" + canPass);

            if (canPass) {
                boolean added = edge.addVehicle(flow);
                System.out.println("DEBUG: addVehicle result=" + added);
                if (added) {
                    flow.setCurrentEdge(edge);
                    flow.setState(TrafficFlow.FlowState.ACTIVE);
                    System.out.println("Traffic flow " + flow.getFlowId() + " successfully entered road " + edge.getId());
                }
            }
        }
    }

    /**
     * Try to move flow to the next node
     */
    private void tryMoveToNextNode(TrafficFlow flow) {
        Edge currentEdge = flow.getCurrentEdge();
        if (currentEdge == null) {
            return;
        }

        // Check if enough time has been spent on the current edge (using actual speed with congestion)
        double requiredTime = currentEdge.getActualTravelTime() * 60; // convert to seconds
        double occupancyRate = currentEdge.getOccupancyRate();
        System.out.println("DEBUG tryMoveToNextNode: " + flow.getFlowId() +
            " timeOnEdge=" + flow.getTimeOnCurrentEdge() +
            " requiredTime=" + requiredTime +
            " occupancy=" + String.format("%.1f%%", occupancyRate * 100) +
            " actualSpeed=" + String.format("%.1f", currentEdge.getActualSpeed()) + "km/h" +
            " state=" + flow.getState());

        if (flow.getTimeOnCurrentEdge() >= requiredTime) {
            // Try to move to the next node
            Node currentNode = flow.getCurrentNode();
            Node nextNode = flow.getNextNode();

            boolean canPassResult = (nextNode != null && canPass(currentNode, nextNode));
            System.out.println("DEBUG: currentNode=" + (currentNode != null ? currentNode.getId() : "null") +
                " nextNode=" + (nextNode != null ? nextNode.getId() : "null") +
                " canPass=" + canPassResult);

            if (canPassResult) {
                // Can pass the traffic light, remove from current edge
                currentEdge.removeVehicle();

                // Move to next node
                flow.moveToNextNode();
                flow.setTimeOnCurrentEdge(0); // reset time counter

                // Check if destination has been reached
                if (flow.hasReachedDestination()) {
                    flow.setState(TrafficFlow.FlowState.COMPLETED);
                    System.out.println("Traffic flow " + flow.getFlowId() + " has completed its journey!");
                    System.out.println("DEBUG: flow state set to COMPLETED, isCompleted=" + flow.isCompleted() +
                        " currentPathIndex=" + flow.getCurrentPathIndex() + " pathSize=" + flow.getPath().size());
                } else {
                    // Still more road ahead, try to enter next edge
                    Node newNextNode = flow.getNextNode();
                    if (newNextNode != null) {
                        Edge newEdge = flow.getCurrentNode().getEdgeTo(newNextNode);
                        if (newEdge != null && !newEdge.isFull()) {
                            newEdge.addVehicle(flow);
                            flow.setCurrentEdge(newEdge);
                            flow.setState(TrafficFlow.FlowState.ACTIVE);
                        } else {
                            // Next edge is full, temporarily blocked
                            flow.setState(TrafficFlow.FlowState.BLOCKED);
                        }
                    } else {
                        // No next node, destination has been reached
                        flow.setState(TrafficFlow.FlowState.COMPLETED);
                        System.out.println("Traffic flow " + flow.getFlowId() + " has reached destination!");
                    }
                }
            } else {
                // Cannot pass (red light or direction mismatch), set to blocked state
                flow.setState(TrafficFlow.FlowState.BLOCKED);
            }
        }
    }

    /**
     * Check if a flow can pass from one node to another
     */
    private boolean canPass(Node from, Node to) {
        // If source is not an intersection, can pass
        if (from.getType() != NodeType.INTERSECTION) {
            return true;
        }

        // Check traffic light
        TrafficLight light = from.getTrafficLight();
        if (light == null) {
            return true;
        }

        // Simplified implementation: determine direction based on node position
        // TODO: Implement more precise direction determination
        TrafficLight.SignalDirection direction = determineDirection(from, to);
        return light.canPass(direction);
    }

    /**
     * Determine travel direction
     */
    private TrafficLight.SignalDirection determineDirection(Node from, Node to) {
        double dx = to.getX() - from.getX();
        double dy = to.getY() - from.getY();

        // If horizontal movement is greater than vertical, it is east-west
        if (Math.abs(dx) > Math.abs(dy)) {
            return TrafficLight.SignalDirection.EAST_WEST;
        } else {
            return TrafficLight.SignalDirection.NORTH_SOUTH;
        }
    }

    /**
     * Evaluate current efficiency
     */
    private void evaluateEfficiency() {
        List<TrafficFlow> completedFlows = flowManager.getCompletedFlowsList();
        double efficiency = efficiencyCalculator.calculateEfficiency(completedFlows);

        efficiencyCalculator.recordEfficiency(efficiency, currentTime);
        signalController.recordOptimization(efficiency);

        System.out.printf("Time %d s - Efficiency: %.2f%n", currentTime, efficiency);
    }

    /**
     * Get current performance metrics
     */
    public EfficiencyCalculator.PerformanceMetrics getCurrentMetrics() {
        return efficiencyCalculator.calculatePerformanceMetrics(
            flowManager.getActiveFlowsList(),
            flowManager.getCompletedFlowsList()
        );
    }

    /**
     * Set time step
     */
    public void setTimeStep(double timeStep) {
        this.timeStep = Math.max(0.1, Math.min(10.0, timeStep));
    }

    /**
     * Set simulation speed
     */
    public void setSpeedMultiplier(double multiplier) {
        this.speedMultiplier = Math.max(0.1, Math.min(10.0, multiplier));
    }

    /**
     * Randomly generate new traffic flows
     */
    private void generateRandomFlows() {
        if (graph == null) {
            return;
        }

        // Get all boundary nodes
        List<Node> boundaryNodes = graph.getBoundaryNodes();
        if (boundaryNodes.size() < 2) {
            return;
        }

        // Randomly generate 1-3 flows
        int flowCount = random.nextInt(3) + 1;

        for (int i = 0; i < flowCount; i++) {
            // Randomly select different entry and exit points
            Node entry = boundaryNodes.get(random.nextInt(boundaryNodes.size()));
            Node destination;
            do {
                destination = boundaryNodes.get(random.nextInt(boundaryNodes.size()));
            } while (entry.equals(destination));

            // Random number of vehicles: 5-15
            int numberOfCars = random.nextInt(11) + 5;

            try {
                flowManager.createFlow(entry.getId(), destination.getId(), numberOfCars);
                System.out.println("Auto-generated flow: " + entry.getId() + " -> " +
                    destination.getId() + " (" + numberOfCars + " vehicles)");
            } catch (Exception e) {
                System.err.println("Failed to generate flow: " + e.getMessage());
            }
        }
    }

    /**
     * Set whether continuous flow generation is enabled
     */
    public void setContinuousFlowEnabled(boolean enabled) {
        this.continuousFlowEnabled = enabled;
        System.out.println("Continuous flow generation: " + (enabled ? "enabled" : "disabled"));
    }

    /**
     * Set flow generation interval
     */
    public void setFlowGenerationInterval(long interval) {
        this.flowGenerationInterval = Math.max(10, interval); // minimum 10 seconds
        System.out.println("Flow generation interval set to: " + interval + " seconds");
    }

    /**
     * Simulation state enumeration
     */
    public enum SimulationState {
        STOPPED,      // stopped
        INITIALIZED,  // initialized
        RUNNING,      // running
        PAUSED        // paused
    }
}
