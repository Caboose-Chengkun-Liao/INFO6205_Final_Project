package com.traffic.optimization.service;

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
        int totalCars = 0;

        for (TrafficFlow flow : completedFlows) {
            totalTime += flow.getTravelTimeCounter() * flow.getNumberOfCars();
            totalCars += flow.getNumberOfCars();
        }

        return totalCars > 0 ? totalTime / totalCars : 0.0;
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
     * 计算综合性能指标
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
        private double efficiency;           // 效率值
        private int throughput;              // 吞吐量（完成的车辆数）
        private double avgTravelTime;        // 平均旅行时间（秒）
        private double avgSpeed;             // 平均速度（km/h）
        private int activeFlowCount;         // 活跃流数量
        private int completedFlowCount;      // 已完成流数量
        private long timestamp;              // 时间戳

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
                "效率: %.2f | 吞吐量: %d | 平均时间: %.1fs | 平均速度: %.1fkm/h | 活跃: %d | 完成: %d",
                efficiency, throughput, avgTravelTime, avgSpeed, activeFlowCount, completedFlowCount
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
