package com.traffic.optimization.service;

import com.traffic.optimization.model.TrafficFlow;
import lombok.Getter;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Efficiency calculator - calculates traffic system efficiency metrics
 *
 * Efficiency formula: E = Σ(Ni × Li / ti) / Σ(Ni)
 * Where:
 *   E: efficiency value
 *   Ni: number of vehicles in flow i
 *   Li: road length
 *   ti: time for flow to pass between two intersections
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
     * Calculate current system efficiency
     *
     * @param completedFlows list of completed traffic flows
     * @return efficiency value
     */
    public double calculateEfficiency(List<TrafficFlow> completedFlows) {
        if (completedFlows == null || completedFlows.isEmpty()) {
            return 0.0;
        }

        double numeratorSum = 0.0;  // Σ(Ni × Li / ti)
        int denominatorSum = 0;      // Σ(Ni)

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
     * Calculate total system throughput (completed vehicles)
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
        int totalCars = 0;

        for (TrafficFlow flow : completedFlows) {
            totalTime += flow.getTravelTimeCounter() * flow.getNumberOfCars();
            totalCars += flow.getNumberOfCars();
        }

        return totalCars > 0 ? totalTime / totalCars : 0.0;
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
     * Calculate comprehensive performance metrics
     */
    public PerformanceMetrics calculatePerformanceMetrics(
            List<TrafficFlow> activeFlows,
            List<TrafficFlow> completedFlows) {

        double efficiency = calculateEfficiency(completedFlows);
        int throughput = calculateThroughput(completedFlows);
        double avgTravelTime = calculateAverageTravelTime(completedFlows);
        double avgSpeed = calculateAverageSpeed(completedFlows);

        int activeFlowCount = activeFlows != null ? activeFlows.size() : 0;
        int completedFlowCount = completedFlows != null ? completedFlows.size() : 0;

        return new PerformanceMetrics(
            efficiency,
            throughput,
            avgTravelTime,
            avgSpeed,
            activeFlowCount,
            completedFlowCount
        );
    }

    /**
     * Record efficiency
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
     * Get efficiency trend (most recent N records)
     */
    public List<EfficiencyRecord> getEfficiencyTrend(int count) {
        int size = efficiencyHistory.size();
        int start = Math.max(0, size - count);
        return new ArrayList<>(efficiencyHistory.subList(start, size));
    }

    /**
     * Get average efficiency (most recent N records)
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
     * Clear history records
     */
    public void clearHistory() {
        efficiencyHistory.clear();
    }

    /**
     * Performance metrics class
     */
    @Getter
    public static class PerformanceMetrics {
        private double efficiency;           // efficiency value
        private int throughput;              // throughput (completed vehicles)
        private double avgTravelTime;        // average travel time (seconds)
        private double avgSpeed;             // average speed (km/h)
        private int activeFlowCount;         // active flow count
        private int completedFlowCount;      // completed flow count
        private long timestamp;              // timestamp

        public PerformanceMetrics(double efficiency, int throughput,
                                double avgTravelTime, double avgSpeed,
                                int activeFlowCount, int completedFlowCount) {
            this.efficiency = efficiency;
            this.throughput = throughput;
            this.avgTravelTime = avgTravelTime;
            this.avgSpeed = avgSpeed;
            this.activeFlowCount = activeFlowCount;
            this.completedFlowCount = completedFlowCount;
            this.timestamp = System.currentTimeMillis();
        }

        @Override
        public String toString() {
            return String.format(
                "Efficiency: %.2f | Throughput: %d | Avg Time: %.1fs | Avg Speed: %.1fkm/h | Active: %d | Completed: %d",
                efficiency, throughput, avgTravelTime, avgSpeed, activeFlowCount, completedFlowCount
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
