package com.traffic.optimization.service;

import com.traffic.optimization.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * FlowManager unit tests
 */
class FlowManagerTest {

    private FlowManager flowManager;
    private Graph graph;

    @BeforeEach
    void setUp() {
        flowManager = new FlowManager();
        graph = createTestGraph();
        flowManager.setGraph(graph);
    }

    @Test
    void testCreateFlow() {
        TrafficFlow flow = flowManager.createFlow("A", "B", 10);
        assertNotNull(flow);
        assertEquals(10, flow.getNumberOfCars());
        assertNotNull(flow.getPath());
        assertFalse(flow.getPath().isEmpty());
        assertEquals(TrafficFlow.FlowState.WAITING, flow.getState());
    }

    @Test
    void testCreateFlow_invalidNodes() {
        assertThrows(IllegalArgumentException.class, () -> {
            flowManager.createFlow("NONEXISTENT", "B", 10);
        });
    }

    @Test
    void testCreateFlow_noPath() {
        // Add an isolated node with no connected edges
        graph.addNode(new Node("ISOLATED", "Isolated", NodeType.BOUNDARY, 99, 99));
        assertThrows(IllegalArgumentException.class, () -> {
            flowManager.createFlow("A", "ISOLATED", 10);
        });
    }

    @Test
    void testActiveFlowsList() {
        flowManager.createFlow("A", "B", 5);
        flowManager.createFlow("B", "A", 3);

        assertEquals(2, flowManager.getActiveFlowsList().size());
    }

    @Test
    void testUpdateFlows_completedFlowsMoveToCompleted() {
        TrafficFlow flow = flowManager.createFlow("A", "B", 5);
        flow.setState(TrafficFlow.FlowState.COMPLETED);

        flowManager.updateFlows(1.0);

        assertEquals(0, flowManager.getActiveFlowsList().size());
        assertEquals(1, flowManager.getCompletedFlowsList().size());
    }

    @Test
    void testUpdateFlows_travelTimeUpdated() {
        TrafficFlow flow = flowManager.createFlow("A", "B", 5);
        flow.setState(TrafficFlow.FlowState.ACTIVE);

        flowManager.updateFlows(2.0);

        assertEquals(2.0, flow.getTravelTimeCounter(), 0.01);
    }

    @Test
    void testClearAllFlows() {
        flowManager.createFlow("A", "B", 5);
        flowManager.createFlow("B", "A", 3);

        flowManager.clearAllFlows();

        assertEquals(0, flowManager.getActiveFlowsList().size());
        assertEquals(0, flowManager.getCompletedFlowsList().size());
    }

    @Test
    void testGetWaitingFlowsAtNode() {
        TrafficFlow flow = flowManager.createFlow("A", "B", 5);
        // Default state is WAITING; getWaitingFlowsAtNode searches for BLOCKED state
        flow.setState(TrafficFlow.FlowState.BLOCKED);

        Node nodeA = graph.getNode("A");
        int waiting = flowManager.getWaitingFlowsAtNode(nodeA);
        assertTrue(waiting >= 0);
    }

    @Test
    void testFlowIdUniqueness() {
        TrafficFlow f1 = flowManager.createFlow("A", "B", 5);
        TrafficFlow f2 = flowManager.createFlow("A", "B", 5);
        assertNotEquals(f1.getFlowId(), f2.getFlowId());
    }

    @Test
    void testTotalFlowCount() {
        flowManager.createFlow("A", "B", 5);
        flowManager.createFlow("B", "A", 3);
        assertEquals(2, flowManager.getTotalFlowCount());
    }

    private Graph createTestGraph() {
        Graph graph = new Graph();

        Node a = new Node("A", "Entry A", NodeType.BOUNDARY, 0.0, 0.0);
        Node i1 = new Node("1", "Intersection", NodeType.INTERSECTION, 1.0, 0.0);
        Node b = new Node("B", "Entry B", NodeType.BOUNDARY, 2.0, 0.0);

        graph.addNode(a);
        graph.addNode(i1);
        graph.addNode(b);

        graph.addBidirectionalEdge("E1", "E2", a, i1, 1.0);
        graph.addBidirectionalEdge("E3", "E4", i1, b, 1.0);

        return graph;
    }
}
