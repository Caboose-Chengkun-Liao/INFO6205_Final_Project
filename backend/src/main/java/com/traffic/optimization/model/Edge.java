package com.traffic.optimization.model;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import lombok.Data;
import java.util.LinkedList;
import java.util.Queue;

/**
 * Edge class - represents a road
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
     * Road distance (km)
     */
    private double distance;

    /**
     * Road capacity (vehicles/km)
     */
    private double capacityPerKm;

    /**
     * Speed limit (km/h)
     */
    private double speedLimit;

    /**
     * Vehicle queue on the current road
     */
    private Queue<TrafficFlow> vehicleQueue;

    /**
     * Current number of vehicles on the road
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
     * Get total capacity
     */
    public double getTotalCapacity() {
        return capacityPerKm * distance;
    }

    /**
     * Check if road is full
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
     * Get actual speed (considering congestion effect)
     * Dynamically adjusts speed based on road occupancy rate
     */
    public double getActualSpeed() {
        double occupancyRate = getOccupancyRate();

        if (occupancyRate >= 0.9) {
            // Severe congestion: speed reduced to 30%
            return speedLimit * 0.3;
        } else if (occupancyRate >= 0.75) {
            // High congestion: speed reduced to 50%
            return speedLimit * 0.5;
        } else if (occupancyRate >= 0.5) {
            // Moderate congestion: speed reduced to 75%
            return speedLimit * 0.75;
        } else if (occupancyRate >= 0.25) {
            // Light congestion: speed reduced to 90%
            return speedLimit * 0.9;
        }

        // Clear: normal speed
        return speedLimit;
    }

    /**
     * Calculate ideal travel time (minutes) - without congestion
     * Multiplied by 2 to slow vehicle movement for easier observation
     */
    public double getIdealTravelTime() {
        return (distance / speedLimit) * 60 * 2;
    }

    /**
     * Calculate actual travel time (minutes) - with congestion effect
     * Multiplied by 2 to slow vehicle movement for easier observation
     */
    public double getActualTravelTime() {
        double actualSpeed = getActualSpeed();
        if (actualSpeed == 0) {
            return Double.MAX_VALUE; // avoid division by zero
        }
        return (distance / actualSpeed) * 60 * 2;
    }

    /**
     * Add vehicle to road
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
     * Remove vehicle from road
     */
    public TrafficFlow removeVehicle() {
        TrafficFlow flow = vehicleQueue.poll();
        if (flow != null) {
            currentVehicleCount -= flow.getNumberOfCars();
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
