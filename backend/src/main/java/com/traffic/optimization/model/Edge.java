package com.traffic.optimization.model;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import lombok.Data;
import java.util.LinkedList;
import java.util.Queue;

/**
 * Edge class - represents a road segment
 *
 * @author Chengkun Liao, Mingjie Shen
 */
@Data
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
@JsonIgnoreProperties({"vehicleQueue"})
public class Edge {
    /**
     * Edge ID
     */
    private String id;

    /**
     * Source node
     */
    private Node fromNode;

    /**
     * Destination node
     */
    private Node toNode;

    /**
     * Road distance (kilometers)
     */
    private double distance;

    /**
     * Road capacity (vehicles per kilometer)
     */
    private double capacityPerKm;

    /**
     * Speed limit (kilometers per hour)
     */
    private double speedLimit;

    /**
     * Queue of vehicles currently on this road
     */
    private Queue<TrafficFlow> vehicleQueue;

    /**
     * Current number of vehicles on this road
     */
    private int currentVehicleCount;

    /**
     * Constructor
     */
    public Edge(String id, Node fromNode, Node toNode, double distance) {
        this(id, fromNode, toNode, distance, 50.0, 60.0);
    }

    /**
     * Full constructor
     */
    public Edge(String id, Node fromNode, Node toNode, double distance,
                double capacityPerKm, double speedLimit) {
        this.id = id;
        this.fromNode = fromNode;
        this.toNode = toNode;
        this.distance = distance;
        this.capacityPerKm = capacityPerKm;
        this.speedLimit = speedLimit;
        this.vehicleQueue = new LinkedList<>();
        this.currentVehicleCount = 0;
    }

    /**
     * Get total road capacity
     */
    public double getTotalCapacity() {
        return capacityPerKm * distance;
    }

    /**
     * Check whether the road is at full capacity
     */
    public boolean isFull() {
        return currentVehicleCount >= getTotalCapacity();
    }

    /**
     * Get current occupancy rate
     */
    public double getOccupancyRate() {
        return (double) currentVehicleCount / getTotalCapacity();
    }

    /**
     * Get current queue length (size of vehicleQueue)
     * Exposed as int to avoid serializing the entire queue object
     */
    public int getQueueLength() {
        return vehicleQueue == null ? 0 : vehicleQueue.size();
    }

    /**
     * BPR (Bureau of Public Roads) speed-flow model
     *
     * Formula: speed = freeFlowSpeed / (1 + alpha * (V/C)^beta)
     * - alpha = 0.15 (standard parameter controlling congestion sensitivity)
     * - beta  = 4.0  (standard parameter controlling the steepness of speed decay)
     * - V/C   = volume/capacity ratio (i.e., occupancyRate)
     *
     * Compared to step-function decay, BPR provides smooth, continuous speed
     * variation consistent with real traffic flow theory (Highway Capacity Manual)
     */
    private static final double BPR_ALPHA = 0.15;
    private static final double BPR_BETA = 4.0;
    private static final double MIN_SPEED_RATIO = 0.1; // minimum speed is 10% of the speed limit

    public double getActualSpeed() {
        double vcRatio = getOccupancyRate(); // V/C ratio

        // BPR formula
        double speedReduction = 1.0 + BPR_ALPHA * Math.pow(vcRatio, BPR_BETA);
        double actualSpeed = speedLimit / speedReduction;

        // Enforce minimum speed
        return Math.max(actualSpeed, speedLimit * MIN_SPEED_RATIO);
    }

    /**
     * Calculate ideal travel time (minutes) - ignoring congestion
     * Multiplied by 2 to slow vehicle movement and make it easier to observe
     */
    public double getIdealTravelTime() {
        return (distance / speedLimit) * 60 * 2;
    }

    /**
     * Calculate actual travel time (minutes) - accounting for congestion
     * Multiplied by 2 to slow vehicle movement and make it easier to observe
     */
    public double getActualTravelTime() {
        double actualSpeed = getActualSpeed();
        if (actualSpeed == 0) {
            return Double.MAX_VALUE; // avoid division by zero
        }
        return (distance / actualSpeed) * 60 * 2;
    }

    /**
     * Add a vehicle to this road
     */
    public boolean addVehicle(TrafficFlow flow) {
        if (isFull()) {
            return false;
        }
        vehicleQueue.offer(flow);
        currentVehicleCount += flow.getNumberOfCars();
        return true;
    }

    /**
     * Remove the specified traffic flow from this road (ensures the correct flow is removed)
     */
    public boolean removeVehicle(TrafficFlow flow) {
        boolean removed = vehicleQueue.remove(flow);
        if (removed) {
            currentVehicleCount = Math.max(0, currentVehicleCount - flow.getNumberOfCars());
        }
        return removed;
    }

    /**
     * Remove the head-of-queue vehicle from this road (legacy interface compatibility)
     */
    public TrafficFlow removeVehicle() {
        TrafficFlow flow = vehicleQueue.poll();
        if (flow != null) {
            currentVehicleCount = Math.max(0, currentVehicleCount - flow.getNumberOfCars());
        }
        return flow;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Edge edge = (Edge) o;
        return id.equals(edge.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "Edge{" +
                "id='" + id + '\'' +
                ", from=" + fromNode.getId() +
                ", to=" + toNode.getId() +
                ", distance=" + distance + "km" +
                ", vehicles=" + currentVehicleCount + "/" + getTotalCapacity() +
                '}';
    }
}
