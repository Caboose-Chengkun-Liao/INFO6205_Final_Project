package com.traffic.optimization.service;

import com.traffic.optimization.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SimulationEngine unit tests — state machine, stepping, thread safety
 */
class SimulationEngineTest {

    private SimulationEngine engine;
    private FlowManager flowManager;
    private SignalController signalController;
    private EfficiencyCalculator efficiencyCalculator;
    private Graph graph;

    @BeforeEach
    void setUp() {
        flowManager = new FlowManager();
        signalController = new SignalController();
        efficiencyCalculator = new EfficiencyCalculator();

        engine = new SimulationEngine();
        engine.setDependencies(flowManager, signalController, efficiencyCalculator);

        graph = createTestGraph();
        engine.initialize(graph);
        engine.setContinuousFlowEnabled(false);
    }

    @Test
    void testInitialState() {
        assertEquals(SimulationEngine.SimulationState.INITIALIZED, engine.getState());
        assertEquals(0, engine.getCurrentTime());
        assertNotNull(engine.getGraph());
    }

    @Test
    void testStartFromInitialized() {
        engine.start();
        assertEquals(SimulationEngine.SimulationState.RUNNING, engine.getState());
    }

    @Test
    void testPauseAndResume() {
        engine.start();
        engine.pause();
        assertEquals(SimulationEngine.SimulationState.PAUSED, engine.getState());

        engine.start();
        assertEquals(SimulationEngine.SimulationState.RUNNING, engine.getState());
    }

    @Test
    void testStop() {
        engine.start();
        engine.stop();
        assertEquals(SimulationEngine.SimulationState.STOPPED, engine.getState());
        assertEquals(0, engine.getCurrentTime());
    }

    @Test
    void testReset() {
        engine.start();
        engine.step();
        engine.step();
        assertTrue(engine.getCurrentTime() > 0);

        engine.reset();
        assertEquals(SimulationEngine.SimulationState.INITIALIZED, engine.getState());
        assertEquals(0, engine.getCurrentTime());
    }

    @Test
    void testStepIncrementsTime() {
        engine.start();
        long before = engine.getCurrentTime();
        engine.step();
        assertTrue(engine.getCurrentTime() > before);
    }

    @Test
    void testStepRequiresRunning() {
        // INITIALIZED state — step should not advance time
        long before = engine.getCurrentTime();
        engine.step();
        assertEquals(before, engine.getCurrentTime());
    }

    @Test
    void testMultipleSteps() {
        engine.start();
        for (int i = 0; i < 10; i++) {
            engine.step();
        }
        assertEquals(10, engine.getCurrentTime());
    }

    @Test
    void testGetCurrentMetrics() {
        engine.start();
        engine.step();
        var metrics = engine.getCurrentMetrics();
        assertNotNull(metrics);
    }

    @Test
    void testConcurrentStepSafety() throws InterruptedException {
        engine.start();

        Thread t1 = new Thread(() -> {
            for (int i = 0; i < 50; i++) engine.step();
        });
        Thread t2 = new Thread(() -> {
            for (int i = 0; i < 50; i++) engine.step();
        });

        t1.start();
        t2.start();
        t1.join();
        t2.join();

        // synchronized step() should give exactly 100
        assertEquals(100, engine.getCurrentTime());
    }

    private Graph createTestGraph() {
        Graph g = new Graph();
        Node a = new Node("A", "Entry A", NodeType.BOUNDARY, 0.0, 0.0);
        Node b = new Node("1", "Intersection 1", NodeType.INTERSECTION, 1.0, 0.0);
        Node c = new Node("2", "Intersection 2", NodeType.INTERSECTION, 2.0, 0.0);
        Node d = new Node("B", "Entry B", NodeType.BOUNDARY, 3.0, 0.0);
        g.addNode(a); g.addNode(b); g.addNode(c); g.addNode(d);
        g.addBidirectionalEdge("E1","E2", a, b, 1.0);
        g.addBidirectionalEdge("E3","E4", b, c, 1.5);
        g.addBidirectionalEdge("E5","E6", c, d, 1.0);
        return g;
    }
}
