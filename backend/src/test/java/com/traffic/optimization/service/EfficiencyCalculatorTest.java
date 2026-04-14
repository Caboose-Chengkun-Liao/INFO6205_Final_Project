package com.traffic.optimization.service;

import com.traffic.optimization.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * EfficiencyCalculator 单元测试
 */
class EfficiencyCalculatorTest {

    private EfficiencyCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new EfficiencyCalculator();
    }

    @Test
    void testCalculateEfficiency_emptyList() {
        assertEquals(0.0, calculator.calculateEfficiency(null));
        assertEquals(0.0, calculator.calculateEfficiency(new ArrayList<>()));
    }

    @Test
    void testCalculateEfficiency_singleFlow() {
        Node a = new Node("A", "A", NodeType.BOUNDARY, 0, 0);
        Node b = new Node("B", "B", NodeType.BOUNDARY, 1, 0);

        // 创建边以确保 path 有距离
        Edge edge = new Edge("E1", a, b, 5.0); // 5km 道路
        a.addOutgoingEdge(edge);
        b.addIncomingEdge(edge);

        TrafficFlow flow = new TrafficFlow("F1", a, b, 10);
        flow.setPath(List.of(a, b)); // totalDistance = 5.0km

        // 需要 ACTIVE 状态才能累积时间
        flow.setState(TrafficFlow.FlowState.ACTIVE);
        for (int i = 0; i < 3600; i++) {
            flow.updateTravelTime(1.0);
        }
        flow.setState(TrafficFlow.FlowState.COMPLETED);

        double efficiency = calculator.calculateEfficiency(List.of(flow));
        assertTrue(efficiency > 0, "效率应大于0, totalDistance=" + flow.getTotalDistance()
            + ", travelTime=" + flow.getTravelTimeCounter());
    }

    @Test
    void testCalculateAverageTravelTime_noDoubleCount() {
        Node a = new Node("A", "A", NodeType.BOUNDARY, 0, 0);
        Node b = new Node("B", "B", NodeType.BOUNDARY, 1, 0);

        TrafficFlow flow1 = new TrafficFlow("F1", a, b, 10);
        flow1.setState(TrafficFlow.FlowState.ACTIVE);
        for (int i = 0; i < 100; i++) flow1.updateTravelTime(1.0);

        TrafficFlow flow2 = new TrafficFlow("F2", a, b, 20);
        flow2.setState(TrafficFlow.FlowState.ACTIVE);
        for (int i = 0; i < 200; i++) flow2.updateTravelTime(1.0);

        // 平均旅行时间 = (100 + 200) / 2 = 150（按 flow 数量，不是车辆数量）
        double avg = calculator.calculateAverageTravelTime(List.of(flow1, flow2));
        assertEquals(150.0, avg, 0.01, "平均旅行时间应为 (100+200)/2=150，不应乘车辆数");
    }

    @Test
    void testCalculateAverageSpeed() {
        Node a = new Node("A", "A", NodeType.BOUNDARY, 0, 0);
        Node b = new Node("B", "B", NodeType.BOUNDARY, 1, 0);

        TrafficFlow flow = new TrafficFlow("F1", a, b, 5);
        flow.setPath(List.of(a, b)); // totalDistance calculated from path
        flow.setState(TrafficFlow.FlowState.ACTIVE);

        // 1小时旅行
        for (int i = 0; i < 3600; i++) {
            flow.updateTravelTime(1.0);
        }

        double speed = calculator.calculateAverageSpeed(List.of(flow));
        // speed = totalDistance / (3600/3600) = totalDistance km/h
        assertTrue(speed >= 0);
    }

    @Test
    void testCalculateThroughput() {
        Node a = new Node("A", "A", NodeType.BOUNDARY, 0, 0);
        Node b = new Node("B", "B", NodeType.BOUNDARY, 1, 0);

        TrafficFlow flow1 = new TrafficFlow("F1", a, b, 10);
        flow1.setCompletedCars(10);

        TrafficFlow flow2 = new TrafficFlow("F2", a, b, 20);
        flow2.setCompletedCars(20);

        int throughput = calculator.calculateThroughput(List.of(flow1, flow2));
        assertEquals(30, throughput);
    }

    @Test
    void testRecordEfficiency() {
        for (int i = 0; i < 5; i++) {
            calculator.recordEfficiency(i * 10.0, i * 100L);
        }

        List<EfficiencyCalculator.EfficiencyRecord> trend = calculator.getEfficiencyTrend(3);
        assertEquals(3, trend.size());
        assertEquals(40.0, trend.get(2).getEfficiency(), 0.01);
    }

    @Test
    void testRecordEfficiency_maxSize() {
        // 超过1000条记录时应该自动清理旧数据
        for (int i = 0; i < 1050; i++) {
            calculator.recordEfficiency(i, i);
        }
        assertTrue(calculator.getEfficiencyHistory().size() <= 1000);
    }

    @Test
    void testGetAverageEfficiency() {
        calculator.recordEfficiency(10.0, 100);
        calculator.recordEfficiency(20.0, 200);
        calculator.recordEfficiency(30.0, 300);

        double avg = calculator.getAverageEfficiency(3);
        assertEquals(20.0, avg, 0.01);
    }

    @Test
    void testClearHistory() {
        calculator.recordEfficiency(10.0, 100);
        calculator.clearHistory();
        assertEquals(0, calculator.getEfficiencyHistory().size());
    }

    // ==================== Network-level metrics (new) ====================

    /** 构造一个 graph，edges 占用率分别为 ratios 数组指定的值 */
    private Graph graphWithOccupancies(double... ratios) {
        Graph g = new Graph();
        for (int i = 0; i < ratios.length; i++) {
            Node u = new Node("U" + i, "U" + i, NodeType.INTERSECTION, i, 0);
            Node v = new Node("V" + i, "V" + i, NodeType.INTERSECTION, i + 1, 0);
            g.addNode(u); g.addNode(v);
            // capacity = 1km × 50/km = 50
            Edge e = new Edge("E" + i, u, v, 1.0);
            e.setCurrentVehicleCount((int) Math.round(ratios[i] * 50));
            g.addEdge(e);
        }
        return g;
    }

    @Test
    void testCalculateNetworkOccupancy_avgOfEdges() {
        Graph g = graphWithOccupancies(0.1, 0.3, 0.7);
        double occ = calculator.calculateNetworkOccupancy(g);
        // avg = (0.1 + 0.3 + 0.7) / 3 = 0.3667
        assertEquals(0.3667, occ, 0.01);
    }

    @Test
    void testCalculateNetworkOccupancy_emptyGraph() {
        assertEquals(0.0, calculator.calculateNetworkOccupancy(null));
        assertEquals(0.0, calculator.calculateNetworkOccupancy(new Graph()));
    }

    @Test
    void testCalculateCongestedEdgeRatio() {
        // 占用率 0.1 / 0.3 / 0.7  → threshold=0.5 时只有 1 条算拥堵 → 1/3
        Graph g = graphWithOccupancies(0.1, 0.3, 0.7);
        assertEquals(1.0 / 3, calculator.calculateCongestedEdgeRatio(g, 0.5), 0.01);

        // threshold=0.2 时有 2 条算拥堵 → 2/3
        assertEquals(2.0 / 3, calculator.calculateCongestedEdgeRatio(g, 0.2), 0.01);
    }

    @Test
    void testCalculateAverageQueueLength_empty() {
        Graph g = graphWithOccupancies(0.5, 0.5);
        // queue 里没有被 offer 任何 flow，默认 0
        assertEquals(0.0, calculator.calculateAverageQueueLength(g), 0.01);
    }

    @Test
    void testCalculateStoppedVehicleRate() {
        Node a = new Node("A", "A", NodeType.BOUNDARY, 0, 0);
        Node b = new Node("B", "B", NodeType.BOUNDARY, 1, 0);

        TrafficFlow active = new TrafficFlow("F1", a, b, 10);
        active.setState(TrafficFlow.FlowState.ACTIVE);
        TrafficFlow blocked = new TrafficFlow("F2", a, b, 5);
        blocked.setState(TrafficFlow.FlowState.BLOCKED);

        // 5 blocked out of 15 total = 0.3333
        double rate = calculator.calculateStoppedVehicleRate(List.of(active, blocked));
        assertEquals(5.0 / 15, rate, 0.01);
    }

    @Test
    void testCalculateStoppedVehicleRate_allActive() {
        Node a = new Node("A", "A", NodeType.BOUNDARY, 0, 0);
        Node b = new Node("B", "B", NodeType.BOUNDARY, 1, 0);

        TrafficFlow f = new TrafficFlow("F1", a, b, 10);
        f.setState(TrafficFlow.FlowState.ACTIVE);

        assertEquals(0.0, calculator.calculateStoppedVehicleRate(List.of(f)), 0.01);
    }

    @Test
    void testCalculateSpeedReductionRatio() {
        // 低占用率(0.1)下，实际速度接近限速，比值接近 1.0
        Graph g = graphWithOccupancies(0.1, 0.1);
        double ratio = calculator.calculateSpeedReductionRatio(g);
        assertTrue(ratio > 0.9 && ratio <= 1.0, "Low-occupancy speed ratio should be > 0.9, got " + ratio);

        // 高占用率(1.0)下，速度大幅降低
        Graph g2 = graphWithOccupancies(1.0, 1.0);
        double ratio2 = calculator.calculateSpeedReductionRatio(g2);
        assertTrue(ratio2 < ratio, "High occupancy should reduce speed ratio");
    }

    @Test
    void testPerformanceMetrics_extendedWithNetworkFields() {
        Graph g = graphWithOccupancies(0.2, 0.8);
        Node a = new Node("A", "A", NodeType.BOUNDARY, 10, 0);
        Node b = new Node("B", "B", NodeType.BOUNDARY, 11, 0);

        TrafficFlow active = new TrafficFlow("F1", a, b, 5);
        active.setState(TrafficFlow.FlowState.ACTIVE);

        var metrics = calculator.calculatePerformanceMetrics(g, List.of(active), List.of());

        // congestedEdgeRatio: 1 edge > 0.5 out of 2 = 0.5
        assertEquals(0.5, metrics.getCongestedEdgeRatio(), 0.01);
        // networkOccupancy: (0.2 + 0.8) / 2 = 0.5
        assertEquals(0.5, metrics.getNetworkOccupancy(), 0.01);
        // stoppedVehicleRate: 0 blocked out of 5 = 0
        assertEquals(0.0, metrics.getStoppedVehicleRate(), 0.01);
    }

    // ==================== Legacy ====================

    @Test
    void testPerformanceMetrics() {
        Node a = new Node("A", "A", NodeType.BOUNDARY, 0, 0);
        Node b = new Node("B", "B", NodeType.BOUNDARY, 1, 0);

        TrafficFlow active = new TrafficFlow("F1", a, b, 5);
        TrafficFlow completed = new TrafficFlow("F2", a, b, 10);
        completed.setCompletedCars(10);

        var metrics = calculator.calculatePerformanceMetrics(
            List.of(active), List.of(completed)
        );

        assertEquals(1, metrics.getActiveFlowCount());
        assertEquals(1, metrics.getCompletedFlowCount());
        assertEquals(10, metrics.getThroughput());
        assertNotNull(metrics.toString());
    }
}
