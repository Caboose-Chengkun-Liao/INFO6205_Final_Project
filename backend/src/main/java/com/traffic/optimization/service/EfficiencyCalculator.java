package com.traffic.optimization.service;

import com.traffic.optimization.model.Edge;
import com.traffic.optimization.model.Graph;
import com.traffic.optimization.model.TrafficFlow;
import lombok.Getter;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 效率计算器 - 计算交通系统的效率指标
 *
 * 效率公式: E = Σ(Ni × Li / ti) / Σ(Ni)
 * 其中:
 *   E: 效率值
 *   Ni: 流i中的车辆数量
 *   Li: 道路长度
 *   ti: 流通过两个路口的时间
 *
 * @author Chengkun Liao, Mingjie Shen
 */
@Service
@Getter
public class EfficiencyCalculator {

    /**
     * 效率历史记录
     */
    private List<EfficiencyRecord> efficiencyHistory;

    /**
     * 构造函数
     */
    public EfficiencyCalculator() {
        this.efficiencyHistory = new ArrayList<>();
    }

    /**
     * 计算当前系统效率
     *
     * @param completedFlows 已完成的交通流列表
     * @return 效率值
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
            double ti = flow.getTravelTimeCounter() / 3600.0; // 转换为小时

            if (ti > 0) {
                numeratorSum += (Ni * Li / ti);
                denominatorSum += Ni;
            }
        }

        return denominatorSum > 0 ? numeratorSum / denominatorSum : 0.0;
    }

    /**
     * 计算系统总吞吐量（完成的车辆数）
     */
    public int calculateThroughput(List<TrafficFlow> completedFlows) {
        int totalCars = 0;
        for (TrafficFlow flow : completedFlows) {
            totalCars += flow.getCompletedCars();
        }
        return totalCars;
    }

    /**
     * 计算平均旅行时间
     */
    public double calculateAverageTravelTime(List<TrafficFlow> completedFlows) {
        if (completedFlows == null || completedFlows.isEmpty()) {
            return 0.0;
        }

        double totalTime = 0.0;
        int flowCount = 0;

        for (TrafficFlow flow : completedFlows) {
            // travelTimeCounter 已经是该 flow 的总旅行时间（秒），无需再乘车辆数
            totalTime += flow.getTravelTimeCounter();
            flowCount++;
        }

        return flowCount > 0 ? totalTime / flowCount : 0.0;
    }

    /**
     * 计算平均速度
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
     * 计算综合性能指标（基础版本，无 Graph 上下文）
     */
    public PerformanceMetrics calculatePerformanceMetrics(
            List<TrafficFlow> activeFlows,
            List<TrafficFlow> completedFlows) {
        return calculatePerformanceMetrics(null, activeFlows, completedFlows);
    }

    /**
     * 计算综合性能指标（含网络级指标）
     * 新增 5 个网络级指标: networkOccupancy / congestedEdgeRatio / avgQueueLength
     *                    / stoppedVehicleRate / speedReductionRatio
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
     * 网络平均占用率 [0,1]
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
     * 拥堵边比例 — 占用率超过 threshold 的边所占比例 [0,1]
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
     * 全网平均排队长度（vehicles）
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
     * 停滞车辆比例 [0,1] — BLOCKED 流中的车辆数 / 活跃流总车辆数
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
     * 速度流畅率 [0,1] — 全网 actualSpeed 总和 / speedLimit 总和（值越高越流畅）
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
     * 记录效率
     */
    public void recordEfficiency(double efficiency, long timestamp) {
        EfficiencyRecord record = new EfficiencyRecord(timestamp, efficiency);
        efficiencyHistory.add(record);

        // 只保留最近1000条记录
        if (efficiencyHistory.size() > 1000) {
            efficiencyHistory.remove(0);
        }
    }

    /**
     * 获取效率趋势（最近N条记录）
     */
    public List<EfficiencyRecord> getEfficiencyTrend(int count) {
        int size = efficiencyHistory.size();
        int start = Math.max(0, size - count);
        return new ArrayList<>(efficiencyHistory.subList(start, size));
    }

    /**
     * 获取平均效率（最近N条记录）
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
     * 清空历史记录
     */
    public void clearHistory() {
        efficiencyHistory.clear();
    }

    /**
     * 性能指标类
     */
    @Getter
    public static class PerformanceMetrics {
        // Legacy fields
        private double efficiency;           // 效率值
        private int throughput;              // 吞吐量（完成的车辆数）
        private double avgTravelTime;        // 平均旅行时间（秒）
        private double avgSpeed;             // 平均速度（km/h）
        private int activeFlowCount;         // 活跃流数量
        private int completedFlowCount;      // 已完成流数量
        private long timestamp;              // 时间戳

        // Network-level metrics (new)
        private double networkOccupancy;     // [0,1] 全网平均占用率
        private double congestedEdgeRatio;   // [0,1] 拥堵边比例（occupancy > 0.5）
        private double avgQueueLength;       // 全网平均排队长度
        private double stoppedVehicleRate;   // [0,1] 停滞车辆比例
        private double speedReductionRatio;  // [0,1] 速度流畅率（越高越好）

        /** Legacy constructor — for backward compatibility */
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
                "效率: %.2f | 吞吐量: %d | 平均时间: %.1fs | 平均速度: %.1fkm/h | 活跃: %d | 完成: %d | 占用: %.0f%% | 拥堵边: %.0f%% | 队列: %.1f | 停滞: %.0f%% | 流畅: %.0f%%",
                efficiency, throughput, avgTravelTime, avgSpeed, activeFlowCount, completedFlowCount,
                networkOccupancy * 100, congestedEdgeRatio * 100, avgQueueLength,
                stoppedVehicleRate * 100, speedReductionRatio * 100
            );
        }
    }

    /**
     * 效率记录类
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
