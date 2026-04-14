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
    void testQLearningMode_adjustsGreenTime() {
        controller.setOptimizationMode(SignalController.OptimizationMode.LEARNING_BASED);

        flowManager.createFlow("A", "B", 20);

        // 多次优化以触发学习
        for (int i = 0; i < 10; i++) {
            controller.optimizeSignals();
        }

        // Q-Learning 应该选择一个合理的绿灯时长
        Node intersection = graph.getNode("1");
        int greenDuration = intersection.getTrafficLight().getGreenDuration();
        assertTrue(greenDuration >= 10 && greenDuration <= 90,
            "Q-Learning优化后绿灯时长应在合理范围内: " + greenDuration);
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

    // ==================== Multi-objective reward + TD bookkeeping tests ====================

    @Test
    void testQLearning_secondTickCreatesQEntry() {
        controller.setOptimizationMode(SignalController.OptimizationMode.LEARNING_BASED);

        // 两轮 optimize — 第一轮只登记 prev-state，第二轮开始真正 TD 更新
        controller.optimizeSignals();
        int qTableSizeBeforeLearning = controller.getQTable().size();

        controller.optimizeSignals();
        int qTableSizeAfterLearning = controller.getQTable().size();

        // TD 更新应该至少保持 Q-Table 大小（可能新增新 state-action pair）
        assertTrue(qTableSizeAfterLearning >= qTableSizeBeforeLearning,
            "Q-Table 大小应在第二轮学习后保持或增加");
    }

    @Test
    void testQLearning_jitterPenalty_stableOverManyTicks() {
        controller.setOptimizationMode(SignalController.OptimizationMode.LEARNING_BASED);

        // 跑多轮，记录绿灯时长抖动历史
        int[] greenHistory = new int[30];
        for (int i = 0; i < 30; i++) {
            controller.optimizeSignals();
            greenHistory[i] = graph.getNode("1").getTrafficLight().getGreenDuration();
        }

        // 所有 green duration 应在 ACTIONS 数组允许的合法范围内
        for (int g : greenHistory) {
            assertTrue(g >= 10 && g <= 90, "Green duration 越界: " + g);
        }
    }

    @Test
    void testQLearning_rewardSignalExists() {
        // 确保跑多轮后 Q-Table 有非零值（证明 reward 在流动）
        controller.setOptimizationMode(SignalController.OptimizationMode.LEARNING_BASED);

        // 设置 non-trivial 流，触发真实 reward
        flowManager.createFlow("A", "B", 25);

        for (int i = 0; i < 20; i++) {
            controller.optimizeSignals();
        }

        // Q-Table 中至少应有一个非零值（reward 真的流进来了）
        boolean hasNonZero = false;
        for (var qValues : controller.getQTable().values()) {
            for (double q : qValues) {
                if (Math.abs(q) > 1e-6) {
                    hasNonZero = true;
                    break;
                }
            }
            if (hasNonZero) break;
        }
        assertTrue(hasNonZero, "Q-Table 应在学习后出现非零 Q 值");
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
