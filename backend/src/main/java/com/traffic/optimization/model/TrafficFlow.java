package com.traffic.optimization.model;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import lombok.Data;
import java.util.List;

/**
 * Traffic flow class - represents a group of vehicles traveling from entry to destination
 *
 * @author Chengkun Liao, Mingjie Shen
 */
@Data
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "flowId")
@JsonIgnoreProperties({"path"})
public class TrafficFlow {
    /**
     * Flow ID (unique identifier)
     */
    private String flowId;

    /**
     * Entry node
     */
    private Node entryPoint;

    /**
     * Destination node
     */
    private Node destination;

    /**
     * Number of vehicles
     */
    private int numberOfCars;

    /**
     * Travel time counter (seconds)
     */
    private double travelTimeCounter;

    /**
     * Path (sequence of nodes)
     */
    private List<Node> path;

    /**
     * Current path index
     */
    private int currentPathIndex;

    /**
     * Current edge
     */
    private Edge currentEdge;

    /**
     * Time spent on current edge (seconds)
     */
    private double timeOnCurrentEdge;

    /**
     * Flow state
     */
    private FlowState state;

    /**
     * Number of completed vehicles
     */
    private int completedCars;

    /**
     * Total travel distance
     */
    private double totalDistance;

    /**
     * Creation time (simulation time)
     */
    private long createdAt;

    /**
     * Constructor
     */
    public TrafficFlow(String flowId, Node entryPoint, Node destination, int numberOfCars) {
        this.flowId = flowId;
        this.entryPoint = entryPoint;
        this.destination = destination;
        this.numberOfCars = numberOfCars;
        this.travelTimeCounter = 0;
        this.currentPathIndex = 0;
        this.timeOnCurrentEdge = 0;
        this.state = FlowState.WAITING;
        this.completedCars = 0;
        this.totalDistance = 0;
        this.createdAt = System.currentTimeMillis();
    }

    /**
     * Set path
     */
    public void setPath(List<Node> path) {
        this.path = path;
        if (path != null && path.size() > 0) {
            this.totalDistance = calculatePathDistance();
        }
    }

    /**
     * Calculate total path distance
     */
    private double calculatePathDistance() {
        double distance = 0;
        for (int i = 0; i < path.size() - 1; i++) {
            Node from = path.get(i);
            Node to = path.get(i + 1);
            Edge edge = from.getEdgeTo(to);
            if (edge != null) {
                distance += edge.getDistance();
            }
        }
        return distance;
    }

    /**
     * Get current node
     */
    public Node getCurrentNode() {
        if (path == null || currentPathIndex >= path.size()) {
            return null;
        }
        return path.get(currentPathIndex);
    }

    /**
     * Get next node
     */
    public Node getNextNode() {
        if (path == null || currentPathIndex + 1 >= path.size()) {
            return null;
        }
        return path.get(currentPathIndex + 1);
    }

    /**
     * Move to next node
     */
    public boolean moveToNextNode() {
        if (hasReachedDestination()) {
            return false;
        }

        currentPathIndex++;
        timeOnCurrentEdge = 0;

        if (hasReachedDestination()) {
            state = FlowState.COMPLETED;
            completedCars = numberOfCars;
            return true;
        }

        return true;
    }

    /**
     * Check if destination has been reached
     */
    public boolean hasReachedDestination() {
        return currentPathIndex >= path.size() - 1;
    }

    /**
     * Check if flow is completed
     */
    public boolean isCompleted() {
        return state == FlowState.COMPLETED;
    }

    /**
     * Get average speed (km/h)
     */
    public double getAverageSpeed() {
        if (travelTimeCounter == 0) {
            return 0;
        }
        // distance / (time/3600)
        double travelTimeHours = travelTimeCounter / 3600.0;
        return totalDistance / travelTimeHours;
    }

    /**
     * Get efficiency metric (used to calculate E value)
     * E_i = Ni × Li / ti
     */
    public double getEfficiencyMetric() {
        if (travelTimeCounter == 0 || !isCompleted()) {
            return 0;
        }
        return (numberOfCars * totalDistance) / (travelTimeCounter / 3600.0);
    }

    /**
     * Update travel time (called each second)
     */
    public void updateTravelTime(double deltaTime) {
        if (state == FlowState.ACTIVE || state == FlowState.BLOCKED) {
            travelTimeCounter += deltaTime;
            timeOnCurrentEdge += deltaTime;
        }
    }

    /**
     * Flow state enumeration
     */
    public enum FlowState {
        WAITING,    // waiting to enter the network
        ACTIVE,     // moving in the network
        BLOCKED,    // blocked (road full or red light)
        COMPLETED   // completed
    }

    @Override
    public String toString() {
        return "TrafficFlow{" +
                "id='" + flowId + '\'' +
                ", from=" + entryPoint.getId() +
                ", to=" + destination.getId() +
                ", cars=" + numberOfCars +
                ", state=" + state +
                ", time=" + String.format("%.1f", travelTimeCounter) + "s" +
                '}';
    }
}
