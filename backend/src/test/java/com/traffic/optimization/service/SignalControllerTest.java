package com.traffic.optimization.service;

import com.traffic.optimization.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SignalController unit tests - verifies the three optimization modes
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
        // Initial traffic light state
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

        // Green duration should not change in fixed timing mode
        assertEquals(greenBefore, intersection.getTrafficLight().getGreenDuration());
    }

    @Test
    void testWebsterMode_adjustsGreenTime() {
        controller.setOptimizationMode(SignalController.OptimizationMode.TRAFFIC_ADAPTIVE);

        // Create some traffic flows to generate demand
        flowManager.createFlow("A", "B", 30);

        controller.optimizeSignals();

        // Webster mode should adjust green duration
        Node intersection = graph.getNode("1");
        int greenDuration = intersection.getTrafficLight().getGreenDuration();
        assertTrue(greenDuration >= 10 && greenDuration <= 90,
            "Green duration after Webster optimization should be in the 10-90s range: " + greenDuration);
    }

    @Test
    void testGreenWaveMode_alignsCycle() {
        controller.setOptimizationMode(SignalController.OptimizationMode.GREEN_WAVE);
        controller.optimizeSignals();

        // After green wave coordination all intersections should share the same cycle length
        int firstCycle = -1;
        for (Node node : graph.getIntersectionNodes()) {
            TrafficLight light = node.getTrafficLight();
            if (light == null) continue;
            if (firstCycle == -1) firstCycle = light.getCycleLength();
            else assertEquals(firstCycle, light.getCycleLength(),
                "All intersections should have the same cycle length in green wave mode");
        }
        assertTrue(firstCycle > 0, "Cycle length should be positive");
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
        // Should not throw an exception for an invalid node ID
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

        // The first optimize call sets 35/15 timing and aligns phases
        controller.optimizeSignals();
        TrafficLight light = graph.getNode("1").getTrafficLight();
        int remainingAfterInit = light.getRemainingTime();
        int greenEW = light.getGreenDurationEW();
        int greenNS = light.getGreenDurationNS();

        // A second optimize call should not change the phase or green durations
        // (the initialization flag prevents re-synchronization that would disrupt the wave)
        controller.updateSignals(); // simulate 1 second elapsing
        controller.optimizeSignals();
        assertEquals(greenEW, light.getGreenDurationEW(), "EW green should not be modified by a second optimizeSignals call");
        assertEquals(greenNS, light.getGreenDurationNS(), "NS green should not be modified by a second optimizeSignals call");
        // remainingTime is only affected by updateSignals (decrements by 1 each call);
        // optimizeSignals should not jump the phase
        assertEquals(remainingAfterInit - 1, light.getRemainingTime(),
            "optimizeSignals should not interrupt an active countdown");
    }

    @Test
    void testGreenWave_matchesFixedCycle() {
        controller.setOptimizationMode(SignalController.OptimizationMode.GREEN_WAVE);
        controller.optimizeSignals();

        // Green Wave uses the same 20/20 timing as FIXED (cycle=50);
        // the only difference is the phase offset
        for (Node node : graph.getIntersectionNodes()) {
            TrafficLight light = node.getTrafficLight();
            if (light == null) continue;
            assertEquals(20, light.getGreenDurationEW(), "EW green should be 20s");
            assertEquals(20, light.getGreenDurationNS(), "NS green should be 20s");
            assertEquals(50, light.getCycleLength(), "Cycle should be 50s to allow comparison with FIXED");
        }
    }

    @Test
    void testSynchronizeSignals() {
        // Allow some signal updates to change state
        for (int i = 0; i < 20; i++) {
            controller.updateSignals();
        }

        controller.synchronizeSignals();

        // After synchronization all signals should be in EW green
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
