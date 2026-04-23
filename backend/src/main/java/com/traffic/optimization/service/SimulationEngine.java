package com.traffic.optimization.service;

import com.traffic.optimization.model.*;
import lombok.AccessLevel;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simulation engine - responsible for the time-stepped simulation of the entire traffic system
 *
 * @author Chengkun Liao, Mingjie Shen
 */
@Service
@Getter
public class SimulationEngine {

    private static final Logger log = LoggerFactory.getLogger(SimulationEngine.class);

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
     * Simulation state (AtomicReference for thread safety)
     */
    @Getter(AccessLevel.NONE)
    private final AtomicReference<SimulationState> state = new AtomicReference<>(SimulationState.STOPPED);

    /**
     * Current simulation time (seconds) (AtomicLong for thread safety)
     */
    @Getter(AccessLevel.NONE)
    private final AtomicLong currentTime = new AtomicLong(0);

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
     * Time of the last efficiency evaluation
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
     * Time of the last flow generation
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
        this.state.set(SimulationState.STOPPED);
        this.currentTime.set(0);
        this.timeStep = 1.0; // default: 1 second
        this.speedMultiplier = 1.0;
        this.efficiencyEvaluationInterval = 30; // record efficiency data every 30 seconds (for trend charts)
        this.lastEfficiencyEvaluationTime = 0;
        this.continuousFlowEnabled = true; // continuous flow generation enabled by default
        this.flowGenerationInterval = 30; // generate new flows every 30 seconds
        this.lastFlowGenerationTime = 0;
        this.random = new Random();
    }

    /**
     * Manual dependency injection for non-Spring instances (used by ComparisonController)
     */
    public void setDependencies(FlowManager fm, SignalController sc, EfficiencyCalculator ec) {
        this.flowManager = fm;
        this.signalController = sc;
        this.efficiencyCalculator = ec;
    }

    /**
     * Get the current simulation state (thread-safe)
     */
    public SimulationState getState() {
        return state.get();
    }

    /**
     * Get the current simulation time (thread-safe)
     */
    public long getCurrentTime() {
        return currentTime.get();
    }

    /**
     * Initialize the simulation
     */
    public void initialize(Graph graph) {
        this.graph = graph;
        this.flowManager.setGraph(graph);
        this.signalController.setGraph(graph);
        this.signalController.setFlowManager(flowManager);

        this.currentTime.set(0);
        this.state.set(SimulationState.INITIALIZED);

        log.info("Simulation engine initialized");
    }

    /**
     * Start the simulation
     */
    public void start() {
        SimulationState currentState = state.get();
        if (currentState == SimulationState.INITIALIZED || currentState == SimulationState.PAUSED) {
            state.set(SimulationState.RUNNING);

            // Immediately generate the initial batch of flows so the user sees vehicles right away
            if (continuousFlowEnabled) {
                log.info("Generating initial traffic flows...");
                generateRandomFlows();
                generateRandomFlows(); // generate two batches of initial flows
            }

            log.info("Simulation started");
        }
    }

    /**
     * Pause the simulation
     */
    public void pause() {
        if (state.get() == SimulationState.RUNNING) {
            state.set(SimulationState.PAUSED);
            log.info("Simulation paused");
        }
    }

    /**
     * Stop the simulation
     */
    public void stop() {
        state.set(SimulationState.STOPPED);
        currentTime.set(0);
        log.info("Simulation stopped");
    }

    /**
     * Reset the simulation
     */
    public void reset() {
        log.debug("reset() called");

        stop();
        flowManager.clearAllFlows();
        efficiencyCalculator.clearHistory();
        currentTime.set(0);
        lastEfficiencyEvaluationTime = 0;
        state.set(SimulationState.INITIALIZED);
        log.info("Simulation reset");
    }

    /**
     * Execute a single time step (main simulation loop)
     */
    public synchronized void step() {
        if (state.get() != SimulationState.RUNNING) {
            return;
        }

        // 1. Update traffic lights
        signalController.updateSignals();

        // 2. Update traffic flows
        updateTrafficFlows();

        // 3. Update flow manager
        flowManager.updateFlows(timeStep);

        // 4. Advance simulation time
        long time = currentTime.addAndGet((long) timeStep);

        // 5. Continuously generate new flows (if enabled)
        if (continuousFlowEnabled && time - lastFlowGenerationTime >= flowGenerationInterval) {
            generateRandomFlows();
            lastFlowGenerationTime = time;
        }

        // 6. Periodically evaluate efficiency
        if (time - lastEfficiencyEvaluationTime >= efficiencyEvaluationInterval) {
            evaluateEfficiency();
            lastEfficiencyEvaluationTime = time;
        }

        // 7. Periodically optimize signals
        if (time % 300 == 0) { // optimize every 5 minutes
            signalController.optimizeSignals();
        }
    }

    /**
     * Update the movement of all traffic flows
     */
    private void updateTrafficFlows() {
        List<TrafficFlow> activeFlows = flowManager.getActiveFlowsList();

        for (TrafficFlow flow : activeFlows) {
            // Skip completed flows
            if (flow.getState() == TrafficFlow.FlowState.COMPLETED) {
                log.debug("updateTrafficFlows: skipping completed flow {}", flow.getFlowId());
                continue;
            }

            if (flow.getState() == TrafficFlow.FlowState.WAITING) {
                // Attempt to enter the network
                tryEnterNetwork(flow);
            } else if (flow.getState() == TrafficFlow.FlowState.ACTIVE) {
                // Attempt to move to the next node
                tryMoveToNextNode(flow);
            } else if (flow.getState() == TrafficFlow.FlowState.BLOCKED) {
                // Blocked vehicles still need to retry (the signal may have turned green)
                tryMoveToNextNode(flow);
            }
        }
    }

    /**
     * Attempt to let a flow enter the network
     */
    private void tryEnterNetwork(TrafficFlow flow) {
        Node currentNode = flow.getCurrentNode();
        Node nextNode = flow.getNextNode();

        log.debug("tryEnterNetwork: {} currentNode={} nextNode={}",
            flow.getFlowId(),
            currentNode != null ? currentNode.getId() : "null",
            nextNode != null ? nextNode.getId() : "null");

        if (nextNode == null) {
            log.debug("nextNode is null for {}", flow.getFlowId());
            return;
        }

        Edge edge = currentNode.getEdgeTo(nextNode);

        if (edge != null && !edge.isFull()) {
            // Check signal (if the current node is an intersection)
            boolean canPass = canPass(currentNode, nextNode);

            if (canPass) {
                boolean added = edge.addVehicle(flow);
                if (added) {
                    flow.setCurrentEdge(edge);
                    flow.setState(TrafficFlow.FlowState.ACTIVE);
                    log.debug("Traffic flow {} successfully entered road {}", flow.getFlowId(), edge.getId());
                }
            }
        }
    }

    /**
     * Attempt to move a flow to the next node
     */
    private void tryMoveToNextNode(TrafficFlow flow) {
        Edge currentEdge = flow.getCurrentEdge();
        if (currentEdge == null) {
            return;
        }

        // Check whether the flow has spent enough time on the current edge (using actual speed, accounting for congestion)
        double requiredTime = currentEdge.getActualTravelTime() * 60; // convert to seconds
        log.trace("tryMoveToNextNode: {} timeOnEdge={} requiredTime={} occupancy={}%",
            flow.getFlowId(), flow.getTimeOnCurrentEdge(), requiredTime,
            String.format("%.1f", currentEdge.getOccupancyRate() * 100));

        if (flow.getTimeOnCurrentEdge() >= requiredTime) {
            // Attempt to move to the next node
            Node currentNode = flow.getCurrentNode();
            Node nextNode = flow.getNextNode();

            boolean canPassResult = (nextNode != null && canPass(currentNode, nextNode));

            if (canPassResult) {
                // Signal allows passage - remove this flow from the current edge
                currentEdge.removeVehicle(flow);

                // Advance to the next node
                flow.moveToNextNode();
                flow.setTimeOnCurrentEdge(0); // reset time counter

                // moveToNextNode() already handles the COMPLETED state internally
                if (flow.isCompleted()) {
                    log.debug("Traffic flow {} has completed its journey", flow.getFlowId());
                } else {
                    // More road segments remain - attempt to enter the next edge
                    Node newNextNode = flow.getNextNode();
                    if (newNextNode != null) {
                        Edge newEdge = flow.getCurrentNode().getEdgeTo(newNextNode);
                        if (newEdge != null && !newEdge.isFull()) {
                            newEdge.addVehicle(flow);
                            flow.setCurrentEdge(newEdge);
                            flow.setState(TrafficFlow.FlowState.ACTIVE);
                        } else {
                            // Next edge is full - temporarily blocked
                            flow.setState(TrafficFlow.FlowState.BLOCKED);
                        }
                    } else {
                        // No next node - the flow has reached its destination
                        flow.setState(TrafficFlow.FlowState.COMPLETED);
                        flow.setCompletedCars(flow.getNumberOfCars());
                        log.debug("Traffic flow {} has reached its destination", flow.getFlowId());
                    }
                }
            } else {
                // Cannot pass (red light or direction mismatch) - set to blocked
                flow.setState(TrafficFlow.FlowState.BLOCKED);
            }
        }
    }

    /**
     * Check whether a flow can proceed from one node to another
     */
    private boolean canPass(Node from, Node to) {
        // If the source node is not an intersection, always allow passage
        if (from.getType() != NodeType.INTERSECTION) {
            return true;
        }

        // Check the traffic light
        TrafficLight light = from.getTrafficLight();
        if (light == null) {
            return true;
        }

        // Simplified implementation: determine direction from node coordinates
        // TODO: implement more precise direction detection
        TrafficLight.SignalDirection direction = determineDirection(from, to);
        return light.canPass(direction);
    }

    /**
     * Determine the travel direction between two nodes
     */
    private TrafficLight.SignalDirection determineDirection(Node from, Node to) {
        double dx = to.getX() - from.getX();
        double dy = to.getY() - from.getY();

        // If horizontal displacement exceeds vertical, classify as East-West
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

        // If no flows have completed yet, estimate efficiency from active flows
        // to avoid blank charts at the start
        if (efficiency == 0.0) {
            List<TrafficFlow> activeFlows = flowManager.getActiveFlowsList();
            double numeratorSum = 0.0;
            int denominatorSum = 0;
            for (TrafficFlow flow : activeFlows) {
                int Ni = flow.getNumberOfCars();
                double Li = flow.getTotalDistance();
                double ti = flow.getTravelTimeCounter() / 3600.0;
                if (ti > 0 && Li > 0) {
                    numeratorSum += (Ni * Li / ti);
                    denominatorSum += Ni;
                }
            }
            if (denominatorSum > 0) {
                efficiency = numeratorSum / denominatorSum;
            }
        }

        efficiencyCalculator.recordEfficiency(efficiency, currentTime.get());
        signalController.recordOptimization(efficiency);

        log.info("Time {}s - efficiency: {}", currentTime.get(), String.format("%.2f", efficiency));
    }

    /**
     * Get current performance metrics
     */
    public EfficiencyCalculator.PerformanceMetrics getCurrentMetrics() {
        return efficiencyCalculator.calculatePerformanceMetrics(
            graph,
            flowManager.getActiveFlowsList(),
            flowManager.getCompletedFlowsList()
        );
    }

    /**
     * Set the time step
     */
    public void setTimeStep(double timeStep) {
        this.timeStep = Math.max(0.1, Math.min(10.0, timeStep));
    }

    /**
     * Set the simulation speed multiplier
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

        // Generate 5-8 flows at random (more flows keep the roads busy)
        int flowCount = random.nextInt(4) + 5;

        for (int i = 0; i < flowCount; i++) {
            // Randomly choose different entry and exit nodes
            Node entry = boundaryNodes.get(random.nextInt(boundaryNodes.size()));
            Node destination;
            do {
                destination = boundaryNodes.get(random.nextInt(boundaryNodes.size()));
            } while (entry.equals(destination));

            // Random vehicle count: 15-30 (enough load to make congestion visible)
            int numberOfCars = random.nextInt(16) + 15;

            try {
                flowManager.createFlow(entry.getId(), destination.getId(), numberOfCars);
                log.debug("Auto-generated flow: {} -> {} ({} vehicles)", entry.getId(), destination.getId(), numberOfCars);
            } catch (Exception e) {
                log.warn("Failed to generate flow: {}", e.getMessage());
            }
        }
    }

    /**
     * Enable or disable continuous flow generation
     */
    public void setContinuousFlowEnabled(boolean enabled) {
        this.continuousFlowEnabled = enabled;
        log.info("Continuous flow generation: {}", enabled ? "enabled" : "disabled");
    }

    /**
     * Set the flow generation interval
     */
    public void setFlowGenerationInterval(long interval) {
        this.flowGenerationInterval = Math.max(10, interval); // minimum 10 seconds
        log.info("Flow generation interval set to: {}s", interval);
    }

    /**
     * Simulation state enumeration
     */
    public enum SimulationState {
        STOPPED,      // Stopped
        INITIALIZED,  // Initialized
        RUNNING,      // Running
        PAUSED        // Paused
    }
}
