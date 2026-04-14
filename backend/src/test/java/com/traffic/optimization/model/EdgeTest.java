package com.traffic.optimization.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Edge 单元测试 - 验证 BPR 速度模型和队列管理
 */
class EdgeTest {

    private Node nodeA;
    private Node nodeB;
    private Edge edge;

    @BeforeEach
    void setUp() {
        nodeA = new Node("A", "Node A", NodeType.BOUNDARY, 0, 0);
        nodeB = new Node("B", "Node B", NodeType.BOUNDARY, 1, 0);
        edge = new Edge("E1", nodeA, nodeB, 2.0, 50.0, 60.0); // 2km, 50cars/km capacity, 60km/h
    }

    // ========== BPR 速度模型测试 ==========

    @Test
    void testActualSpeed_empty() {
        // 空路时速度应等于限速
        assertEquals(60.0, edge.getActualSpeed(), 0.01);
    }

    @Test
    void testActualSpeed_bprMonotonicallyDecreasing() {
        // BPR 模型：速度应随占用率增加而平滑递减
        double lastSpeed = edge.getActualSpeed();

        for (int i = 1; i <= 10; i++) {
            // 每次添加10辆车
            TrafficFlow flow = new TrafficFlow("F" + i, nodeA, nodeB, 10);
            edge.addVehicle(flow);

            double speed = edge.getActualSpeed();
            assertTrue(speed <= lastSpeed,
                "速度应单调递减: occupancy=" + edge.getOccupancyRate());
            assertTrue(speed > 0, "速度应始终大于0");
            lastSpeed = speed;
        }
    }

    @Test
    void testActualSpeed_neverBelowMinimum() {
        // 即使过饱和，速度也不应低于限速的10%
        // 添加大量车辆使其过饱和
        for (int i = 0; i < 20; i++) {
            TrafficFlow flow = new TrafficFlow("F" + i, nodeA, nodeB, 10);
            // 强制添加，跳过 isFull 检查
            edge.getVehicleQueue().offer(flow);
            edge.setCurrentVehicleCount(edge.getCurrentVehicleCount() + 10);
        }

        double minExpected = 60.0 * 0.1; // 6 km/h
        assertTrue(edge.getActualSpeed() >= minExpected,
            "速度不应低于限速的10%: actual=" + edge.getActualSpeed());
    }

    @Test
    void testActualSpeed_bprFormula() {
        // 验证 BPR 公式: speed = 60 / (1 + 0.15 * (V/C)^4)
        // 在50%占用率时
        int halfCapacity = (int) (edge.getTotalCapacity() / 2);
        TrafficFlow flow = new TrafficFlow("F1", nodeA, nodeB, halfCapacity);
        edge.addVehicle(flow);

        double expected = 60.0 / (1.0 + 0.15 * Math.pow(0.5, 4.0));
        assertEquals(expected, edge.getActualSpeed(), 0.01,
            "BPR公式在50%占用率时计算不正确");
    }

    // ========== 队列管理测试 ==========

    @Test
    void testAddVehicle() {
        TrafficFlow flow = new TrafficFlow("F1", nodeA, nodeB, 5);
        assertTrue(edge.addVehicle(flow));
        assertEquals(5, edge.getCurrentVehicleCount());
    }

    @Test
    void testAddVehicle_full() {
        // 容量 = 50 * 2 = 100
        TrafficFlow bigFlow = new TrafficFlow("F1", nodeA, nodeB, 100);
        assertTrue(edge.addVehicle(bigFlow));

        TrafficFlow extraFlow = new TrafficFlow("F2", nodeA, nodeB, 1);
        assertFalse(edge.addVehicle(extraFlow), "已满时不应该添加成功");
    }

    @Test
    void testRemoveVehicle_specific() {
        TrafficFlow flow1 = new TrafficFlow("F1", nodeA, nodeB, 10);
        TrafficFlow flow2 = new TrafficFlow("F2", nodeA, nodeB, 20);

        edge.addVehicle(flow1);
        edge.addVehicle(flow2);
        assertEquals(30, edge.getCurrentVehicleCount());

        // 移除指定的 flow
        assertTrue(edge.removeVehicle(flow1));
        assertEquals(20, edge.getCurrentVehicleCount());
    }

    @Test
    void testRemoveVehicle_countNeverNegative() {
        TrafficFlow flow = new TrafficFlow("F1", nodeA, nodeB, 5);
        edge.addVehicle(flow);
        edge.removeVehicle(flow);

        // 再次移除不存在的 flow
        edge.removeVehicle(flow);
        assertTrue(edge.getCurrentVehicleCount() >= 0, "车辆计数不应为负");
    }

    // ========== 通行时间测试 ==========

    @Test
    void testTravelTime_increasesWithCongestion() {
        double emptyTime = edge.getActualTravelTime();

        TrafficFlow flow = new TrafficFlow("F1", nodeA, nodeB, 50);
        edge.addVehicle(flow);

        double congestedTime = edge.getActualTravelTime();
        assertTrue(congestedTime > emptyTime,
            "拥堵时通行时间应更长");
    }

    @Test
    void testOccupancyRate() {
        assertEquals(0.0, edge.getOccupancyRate(), 0.01);

        TrafficFlow flow = new TrafficFlow("F1", nodeA, nodeB, 50);
        edge.addVehicle(flow);

        assertEquals(0.5, edge.getOccupancyRate(), 0.01); // 50/100
    }

    @Test
    void testTotalCapacity() {
        assertEquals(100.0, edge.getTotalCapacity(), 0.01); // 50 * 2
    }
}
