package com.traffic.optimization.service;

import com.traffic.optimization.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SignalController 单元测试 - 验证三种优化模式
 */
class SignalControllerTest {

    private SignalController controller;
    private FlowManager flowManager;
    private Graph graph;

    @BeforeEach
    void setUp() {
        controller = new SignalController();
        flowManager = new FlowManager();
        graph = createTestGraph();

        controller.setGraph(graph);
        controller.setFlowManager(flowManager);
        flowManager.setGraph(graph);
    }

    @Test
    void testDefaultMode() {
        assertEquals(SignalController.OptimizationMode.FIXED_TIME, controller.getMode());
    }

    @Test
    void testSetOptimizationMode() {
        controller.setOptimizationMode(SignalController.OptimizationMode.TRAFFIC_ADAPTIVE);
        assertEquals(SignalController.OptimizationMode.TRAFFIC_ADAPTIVE, controller.getMode());
    }

    @Test
    void testUpdateSignals() {
        // 信号灯初始状态
        Node intersection = graph.getNode("1");
        TrafficLight light = intersection.getTrafficLight();
        assertNotNull(light);

        int initialRemaining = light.getRemainingTime();
        controller.updateSignals();
        assertEquals(initialRemaining - 1, light.getRemainingTime());
    }

    @Test
    void testFixedTimeMode_noOptimization() {
        controller.setOptimizationMode(SignalController.OptimizationMode.FIXED_TIME);

        Node intersection = graph.getNode("1");
        int greenBefore = intersection.getTrafficLight().getGreenDuration();

        controller.optimizeSignals();

        // 固定模式下绿灯时长不应变化
        assertEquals(greenBefore, intersection.getTrafficLight().getGreenDuration());
    }

    @Test
    void testWebsterMode_adjustsGreenTime() {
        controller.setOptimizationMode(SignalController.OptimizationMode.TRAFFIC_ADAPTIVE);

        // 创建一些车流来产生交通量
        flowManager.createFlow("A", "B", 30);

        controller.optimizeSignals();

        // Webster 模式应该调整绿灯时长
        Node intersection = graph.getNode("1");
        int greenDuration = intersection.getTrafficLight().getGreenDuration();
        assertTrue(greenDuration >= 10 && greenDuration <= 90,
            "Webster优化后绿灯时长应在10-90秒范围内: " + greenDuration);
    }

    @Test
    void testGreenWaveMode_alignsCycle() {
        controller.setOptimizationMode(SignalController.OptimizationMode.GREEN_WAVE);
        controller.optimizeSignals();

        // 绿波协调后所有路口应该共享相同的 cycle 长度
        int firstCycle = -1;
        for (Node node : graph.getIntersectionNodes()) {
            TrafficLight light = node.getTrafficLight();
            if (light == null) continue;
            if (firstCycle == -1) firstCycle = light.getCycleLength();
            else assertEquals(firstCycle, light.getCycleLength(),
                "绿波模式下所有路口 cycle 应一致");
        }
        assertTrue(firstCycle > 0, "Cycle 长度应为正数");
    }

    @Test
    void testGetAllSignalStatuses() {
        var statuses = controller.getAllSignalStatuses();
        assertFalse(statuses.isEmpty());

        var status = statuses.get(0);
        assertNotNull(status.getNodeId());
        assertNotNull(status.getState());
        assertTrue(status.getGreenDuration() > 0);
    }

    @Test
    void testSetSignalTiming() {
        controller.setSignalTiming("1", 45);

        Node intersection = graph.getNode("1");
        assertEquals(45, intersection.getTrafficLight().getGreenDuration());
    }

    @Test
    void testSetSignalTiming_invalidNode() {
        // 不应该抛异常
        controller.setSignalTiming("NONEXISTENT", 30);
    }

    @Test
    void testRecordOptimization() {
        controller.recordOptimization(85.5);
        controller.recordOptimization(90.0);

        var history = controller.getOptimizationHistory();
        assertEquals(2, history.size());
        assertEquals(90.0, history.get(1).getEfficiency(), 0.01);
    }

    @Test
    void testRecordOptimization_maxSize() {
        for (int i = 0; i < 150; i++) {
            controller.recordOptimization(i);
        }
        assertTrue(controller.getOptimizationHistory().size() <= 100);
    }

    // ==================== Green Wave tests ====================

    @Test
    void testGreenWave_initializesOnlyOnce() {
        controller.setOptimizationMode(SignalController.OptimizationMode.GREEN_WAVE);

        // 首次 optimize 会设置 35/15 并对齐相位
        controller.optimizeSignals();
        TrafficLight light = graph.getNode("1").getTrafficLight();
        int remainingAfterInit = light.getRemainingTime();
        int greenEW = light.getGreenDurationEW();
        int greenNS = light.getGreenDurationNS();

        // 二次 optimize 不应改动相位/绿灯(初始化标记生效,避免打断绿波)
        controller.updateSignals(); // 模拟 1 秒流逝
        controller.optimizeSignals();
        assertEquals(greenEW, light.getGreenDurationEW(), "EW green 不应被二次 optimize 修改");
        assertEquals(greenNS, light.getGreenDurationNS(), "NS green 不应被二次 optimize 修改");
        // remainingTime 仅受 updateSignals 影响(每调 updateSignals 减 1),optimizeSignals 不应跳相位
        assertEquals(remainingAfterInit - 1, light.getRemainingTime(),
            "optimizeSignals 不应打断正在倒计时的相位");
    }

    @Test
    void testGreenWave_matchesFixedCycle() {
        controller.setOptimizationMode(SignalController.OptimizationMode.GREEN_WAVE);
        controller.optimizeSignals();

        // 绿波使用与 FIXED 相同的 20/20 配时(cycle=50),唯一区别是相位 offset
        for (Node node : graph.getIntersectionNodes()) {
            TrafficLight light = node.getTrafficLight();
            if (light == null) continue;
            assertEquals(20, light.getGreenDurationEW(), "EW 绿应为 20s");
            assertEquals(20, light.getGreenDurationNS(), "NS 绿应为 20s");
            assertEquals(50, light.getCycleLength(), "cycle 应为 50s 以便与 FIXED 对照");
        }
    }

    @Test
    void testSynchronizeSignals() {
        // 先让一些信号灯变化
        for (int i = 0; i < 20; i++) {
            controller.updateSignals();
        }

        controller.synchronizeSignals();

        // 同步后所有信号灯应该是东西向绿灯
        for (Node node : graph.getIntersectionNodes()) {
            TrafficLight light = node.getTrafficLight();
            assertEquals(TrafficLight.SignalDirection.EAST_WEST, light.getCurrentDirection());
            assertEquals(TrafficLight.SignalState.GREEN, light.getCurrentState());
        }
    }

    private Graph createTestGraph() {
        Graph graph = new Graph();

        Node a = new Node("A", "Entry A", NodeType.BOUNDARY, 0.0, 0.0);
        Node i1 = new Node("1", "Intersection 1", NodeType.INTERSECTION, 1.0, 1.0);
        Node i2 = new Node("2", "Intersection 2", NodeType.INTERSECTION, 2.0, 1.0);
        Node b = new Node("B", "Entry B", NodeType.BOUNDARY, 3.0, 0.0);

        graph.addNode(a);
        graph.addNode(i1);
        graph.addNode(i2);
        graph.addNode(b);

        graph.addBidirectionalEdge("E1", "E2", a, i1, 1.0);
        graph.addBidirectionalEdge("E3", "E4", i1, i2, 1.5);
        graph.addBidirectionalEdge("E5", "E6", i2, b, 1.0);

        return graph;
    }
}
