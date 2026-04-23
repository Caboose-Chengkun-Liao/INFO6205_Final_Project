package com.traffic.optimization.service;

import com.traffic.optimization.model.Edge;
import com.traffic.optimization.model.Graph;
import com.traffic.optimization.model.TrafficFlow;
import lombok.Getter;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Efficiency calculator - computes traffic system efficiency metrics.
 *
 * Efficiency formula: E = sum(Ni * Li / ti) / sum(Ni)
 * where:
 *   E:  efficiency value
 *   Ni: number of vehicles in flow i
 *   Li: road length
 *   ti: time for the flow to travel between two intersections
 *
 * @author Chengkun Liao, Mingjie Shen
 */
@Service
@Getter
public class EfficiencyCalculator {

    /**
     * Efficiency history records
     */
    private List<EfficiencyRecord> efficiencyHistory;

    /**
     * Constructor
     */
    public EfficiencyCalculator() {
        this.efficiencyHistory = new ArrayList<>();
    }

    /**
     * Calculate the current system efficiency.
     *
     * @param completedFlows list of completed traffic flows
     * @return efficiency value
     */
    public double calculateEfficiency(List<TrafficFlow> completedFlows) {
        if (completedFlows == null || completedFlows.isEmpty()) {
            return 0.0;
        }

        double numeratorSum = 0.0;  // sum(Ni * Li / ti)
        int denominatorSum = 0;      // sum(Ni)

        for (TrafficFlow flow : completedFlows) {
            int Ni = flow.getNumberOfCars();
            double Li = flow.getTotalDistance();
            double ti = flow.getTravelTimeCounter() / 3600.0; // convert to hours

            if (ti > 0) {
                numeratorSum += (Ni * Li / ti);
                denominatorSum += Ni;
            }
        }

        return denominatorSum > 0 ? numeratorSum / denominatorSum : 0.0;
    }

    /**
     * Calculate total system throughput (number of completed vehicles)
     */
    public int calculateThroughput(List<TrafficFlow> completedFlows) {
        int totalCars = 0;
        for (TrafficFlow flow : completedFlows) {
            totalCars += flow.getCompletedCars();
        }
        return totalCars;
    }

    /**
     * Calculate average travel time
     */
    public double calculateAverageTravelTime(List<TrafficFlow> completedFlows) {
        if (completedFlows == null || completedFlows.isEmpty()) {
            return 0.0;
        }

        double totalTime = 0.0;
        int flowCount = 0;

        for (TrafficFlow flow : completedFlows) {
            // travelTimeCounter is already the total travel time for this flow (seconds);
            // there is no need to multiply by the number of vehicles
            totalTime += flow.getTravelTimeCounter();
            flowCount++;
        }

        return flowCount > 0 ? totalTime / flowCount : 0.0;
    }

    /**
     * Calculate average speed
     */
    public double calculateAverageSpeed(List<TrafficFlow> completedFlows) {
        if (completedFlows == null || completedFlows.isEmpty()) {
            return 0.0;
        }

        double totalSpeed = 0.0;
        int count = 0;

        for (TrafficFlow flow : completedFlows) {
            double speed = flow.getAverageSpeed();
            if (speed > 0) {
                totalSpeed += speed;
                count++;
            }
        }

        return count > 0 ? totalSpeed / count : 0.0;
    }

    /**
     * Calculate combined performance metrics (basic version, no Graph context)
     */
    public PerformanceMetrics calculatePerformanceMetrics(
            List<TrafficFlow> activeFlows,
            List<TrafficFlow> completedFlows) {
        return calculatePerformanceMetrics(null, activeFlows, completedFlows);
    }

    /**
     * Calculate combined performance metrics (including network-level metrics).
     * Five new network-level metrics: networkOccupancy / congestedEdgeRatio / avgQueueLength
     *                                 / stoppedVehicleRate / speedReductionRatio
     */
    public PerformanceMetrics calculatePerformanceMetrics(
            Graph graph,
            List<TrafficFlow> activeFlows,
            List<TrafficFlow> completedFlows) {

        double efficiency = calculateEfficiency(completedFlows);
        int throughput = calculateThroughput(completedFlows);
        double avgTravelTime = calculateAverageTravelTime(completedFlows);
        double avgSpeed = calculateAverageSpeed(completedFlows);

        int activeFlowCount = activeFlows != null ? activeFlows.size() : 0;
        int completedFlowCount = completedFlows != null ? completedFlows.size() : 0;

        // Network-level metrics (new)
        double networkOccupancy = calculateNetworkOccupancy(graph);
        double congestedEdgeRatio = calculateCongestedEdgeRatio(graph, 0.5);
        double avgQueueLength = calculateAverageQueueLength(graph);
        double stoppedVehicleRate = calculateStoppedVehicleRate(activeFlows);
        double speedReductionRatio = calculateSpeedReductionRatio(graph);

        return new PerformanceMetrics(
            efficiency, throughput, avgTravelTime, avgSpeed,
            activeFlowCount, completedFlowCount,
            networkOccupancy, congestedEdgeRatio, avgQueueLength,
            stoppedVehicleRate, speedReductionRatio
        );
    }

    // ==================== Network-level metric helpers ====================

    /**
     * Network average occupancy rate [0, 1]
     */
    double calculateNetworkOccupancy(Graph graph) {
        if (graph == null || graph.getEdges() == null || graph.getEdges().isEmpty()) return 0.0;
        double sum = 0.0;
        int n = 0;
        for (Edge e : graph.getEdges()) {
            if (e.getTotalCapacity() > 0) {
                sum += Math.min(1.0, e.getOccupancyRate());
                n++;
            }
        }
        return n == 0 ? 0.0 : sum / n;
    }

    /**
     * Congested edge ratio - proportion of edges with occupancy exceeding the threshold [0, 1]
     */
    double calculateCongestedEdgeRatio(Graph graph, double threshold) {
        if (graph == null || graph.getEdges() == null || graph.getEdges().isEmpty()) return 0.0;
        int total = 0;
        int congested = 0;
        for (Edge e : graph.getEdges()) {
            if (e.getTotalCapacity() > 0) {
                total++;
                if (e.getOccupancyRate() > threshold) congested++;
            }
        }
        return total == 0 ? 0.0 : (double) congested / total;
    }

    /**
     * Network average queue length (vehicles)
     */
    double calculateAverageQueueLength(Graph graph) {
        if (graph == null || graph.getEdges() == null || graph.getEdges().isEmpty()) return 0.0;
        int sum = 0;
        int n = 0;
        for (Edge e : graph.getEdges()) {
            sum += e.getQueueLength();
            n++;
        }
        return n == 0 ? 0.0 : (double) sum / n;
    }

    /**
     * Stopped vehicle rate [0, 1] - vehicles in BLOCKED flows / total vehicles in active flows
     */
    double calculateStoppedVehicleRate(List<TrafficFlow> activeFlows) {
        if (activeFlows == null || activeFlows.isEmpty()) return 0.0;
        int blocked = 0;
        int total = 0;
        for (TrafficFlow f : activeFlows) {
            int n = f.getNumberOfCars();
            total += n;
            if (f.getState() == TrafficFlow.FlowState.BLOCKED) {
                blocked += n;
            }
        }
        return total == 0 ? 0.0 : (double) blocked / total;
    }

    /**
     * Speed fluency ratio [0, 1] - sum of actualSpeed across all edges / sum of speedLimit
     * (higher value means smoother traffic)
     */
    double calculateSpeedReductionRatio(Graph graph) {
        if (graph == null || graph.getEdges() == null || graph.getEdges().isEmpty()) return 0.0;
        double sumActual = 0.0;
        double sumLimit = 0.0;
        for (Edge e : graph.getEdges()) {
            if (e.getSpeedLimit() > 0) {
                sumActual += e.getActualSpeed();
                sumLimit += e.getSpeedLimit();
            }
        }
        return sumLimit == 0 ? 0.0 : sumActual / sumLimit;
    }

    /**
     * Record an efficiency measurement
     */
    public void recordEfficiency(double efficiency, long timestamp) {
        EfficiencyRecord record = new EfficiencyRecord(timestamp, efficiency);
        efficiencyHistory.add(record);

        // Keep only the most recent 1000 records
        if (efficiencyHistory.size() > 1000) {
            efficiencyHistory.remove(0);
        }
    }

    /**
     * Get the efficiency trend (most recent N records)
     */
    public List<EfficiencyRecord> getEfficiencyTrend(int count) {
        int size = efficiencyHistory.size();
        int start = Math.max(0, size - count);
        return new ArrayList<>(efficiencyHistory.subList(start, size));
    }

    /**
     * Get the average efficiency over the most recent N records
     */
    public double getAverageEfficiency(int count) {
        List<EfficiencyRecord> trend = getEfficiencyTrend(count);
        if (trend.isEmpty()) {
            return 0.0;
        }

        double sum = 0.0;
        for (EfficiencyRecord record : trend) {
            sum += record.getEfficiency();
        }

        return sum / trend.size();
    }

    /**
     * Clear all history records
     */
    public void clearHistory() {
        efficiencyHistory.clear();
    }

    /**
     * Performance metrics class
     */
    @Getter
    public static class PerformanceMetrics {
        // Legacy fields
        private double efficiency;           // efficiency value
        private int throughput;              // throughput (number of completed vehicles)
        private double avgTravelTime;        // average travel time (seconds)
        private double avgSpeed;             // average speed (km/h)
        private int activeFlowCount;         // number of active flows
        private int completedFlowCount;      // number of completed flows
        private long timestamp;              // timestamp

        // Network-level metrics (new)
        private double networkOccupancy;     // [0,1] network average occupancy rate
        private double congestedEdgeRatio;   // [0,1] congested edge ratio (occupancy > 0.5)
        private double avgQueueLength;       // network average queue length
        private double stoppedVehicleRate;   // [0,1] stopped vehicle rate
        private double speedReductionRatio;  // [0,1] speed fluency ratio (higher is better)

        /** Legacy constructor - for backward compatibility */
        public PerformanceMetrics(double efficiency, int throughput,
                                double avgTravelTime, double avgSpeed,
                                int activeFlowCount, int completedFlowCount) {
            this(efficiency, throughput, avgTravelTime, avgSpeed,
                 activeFlowCount, completedFlowCount,
                 0.0, 0.0, 0.0, 0.0, 0.0);
        }

        /** Extended constructor with network-level metrics */
        public PerformanceMetrics(double efficiency, int throughput,
                                double avgTravelTime, double avgSpeed,
                                int activeFlowCount, int completedFlowCount,
                                double networkOccupancy, double congestedEdgeRatio,
                                double avgQueueLength, double stoppedVehicleRate,
                                double speedReductionRatio) {
            this.efficiency = efficiency;
            this.throughput = throughput;
            this.avgTravelTime = avgTravelTime;
            this.avgSpeed = avgSpeed;
            this.activeFlowCount = activeFlowCount;
            this.completedFlowCount = completedFlowCount;
            this.networkOccupancy = networkOccupancy;
            this.congestedEdgeRatio = congestedEdgeRatio;
            this.avgQueueLength = avgQueueLength;
            this.stoppedVehicleRate = stoppedVehicleRate;
            this.speedReductionRatio = speedReductionRatio;
            this.timestamp = System.currentTimeMillis();
        }

        @Override
        public String toString() {
            return String.format(
                "Efficiency: %.2f | Throughput: %d | Avg time: %.1fs | Avg speed: %.1fkm/h | Active: %d | Completed: %d | Occupancy: %.0f%% | Congested edges: %.0f%% | Queue: %.1f | Stopped: %.0f%% | Fluency: %.0f%%",
                efficiency, throughput, avgTravelTime, avgSpeed, activeFlowCount, completedFlowCount,
                networkOccupancy * 100, congestedEdgeRatio * 100, avgQueueLength,
                stoppedVehicleRate * 100, speedReductionRatio * 100
            );
        }
    }

    /**
     * Efficiency record class
     */
    @Getter
    public static class EfficiencyRecord {
        private long timestamp;
        private double efficiency;

        public EfficiencyRecord(long timestamp, double efficiency) {
            this.timestamp = timestamp;
            this.efficiency = efficiency;
        }
    }
}
