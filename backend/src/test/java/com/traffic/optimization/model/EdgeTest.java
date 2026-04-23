package com.traffic.optimization.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Edge unit tests - verifies the BPR speed model and queue management
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

    // ========== BPR speed model tests ==========

    @Test
    void testActualSpeed_empty() {
        // Speed on an empty road should equal the speed limit
        assertEquals(60.0, edge.getActualSpeed(), 0.01);
    }

    @Test
    void testActualSpeed_bprMonotonicallyDecreasing() {
        // BPR model: speed should decrease smoothly as occupancy increases
        double lastSpeed = edge.getActualSpeed();

        for (int i = 1; i <= 10; i++) {
            // Add 10 vehicles each iteration
            TrafficFlow flow = new TrafficFlow("F" + i, nodeA, nodeB, 10);
            edge.addVehicle(flow);

            double speed = edge.getActualSpeed();
            assertTrue(speed <= lastSpeed,
                "Speed should decrease monotonically: occupancy=" + edge.getOccupancyRate());
            assertTrue(speed > 0, "Speed should always be greater than 0");
            lastSpeed = speed;
        }
    }

    @Test
    void testActualSpeed_neverBelowMinimum() {
        // Even when oversaturated, speed should not fall below 10% of the speed limit
        // Add a large number of vehicles to create oversaturation
        for (int i = 0; i < 20; i++) {
            TrafficFlow flow = new TrafficFlow("F" + i, nodeA, nodeB, 10);
            // Force-add, bypassing the isFull check
            edge.getVehicleQueue().offer(flow);
            edge.setCurrentVehicleCount(edge.getCurrentVehicleCount() + 10);
        }

        double minExpected = 60.0 * 0.1; // 6 km/h
        assertTrue(edge.getActualSpeed() >= minExpected,
            "Speed should not fall below 10% of speed limit: actual=" + edge.getActualSpeed());
    }

    @Test
    void testActualSpeed_bprFormula() {
        // Verify BPR formula: speed = 60 / (1 + 0.15 * (V/C)^4)
        // At 50% occupancy
        int halfCapacity = (int) (edge.getTotalCapacity() / 2);
        TrafficFlow flow = new TrafficFlow("F1", nodeA, nodeB, halfCapacity);
        edge.addVehicle(flow);

        double expected = 60.0 / (1.0 + 0.15 * Math.pow(0.5, 4.0));
        assertEquals(expected, edge.getActualSpeed(), 0.01,
            "BPR formula produces incorrect result at 50% occupancy");
    }

    // ========== Queue management tests ==========

    @Test
    void testAddVehicle() {
        TrafficFlow flow = new TrafficFlow("F1", nodeA, nodeB, 5);
        assertTrue(edge.addVehicle(flow));
        assertEquals(5, edge.getCurrentVehicleCount());
    }

    @Test
    void testAddVehicle_full() {
        // Capacity = 50 * 2 = 100
        TrafficFlow bigFlow = new TrafficFlow("F1", nodeA, nodeB, 100);
        assertTrue(edge.addVehicle(bigFlow));

        TrafficFlow extraFlow = new TrafficFlow("F2", nodeA, nodeB, 1);
        assertFalse(edge.addVehicle(extraFlow), "Adding to a full road should fail");
    }

    @Test
    void testRemoveVehicle_specific() {
        TrafficFlow flow1 = new TrafficFlow("F1", nodeA, nodeB, 10);
        TrafficFlow flow2 = new TrafficFlow("F2", nodeA, nodeB, 20);

        edge.addVehicle(flow1);
        edge.addVehicle(flow2);
        assertEquals(30, edge.getCurrentVehicleCount());

        // Remove a specific flow
        assertTrue(edge.removeVehicle(flow1));
        assertEquals(20, edge.getCurrentVehicleCount());
    }

    @Test
    void testRemoveVehicle_countNeverNegative() {
        TrafficFlow flow = new TrafficFlow("F1", nodeA, nodeB, 5);
        edge.addVehicle(flow);
        edge.removeVehicle(flow);

        // Attempt to remove the same flow again
        edge.removeVehicle(flow);
        assertTrue(edge.getCurrentVehicleCount() >= 0, "Vehicle count should not go negative");
    }

    // ========== Travel time tests ==========

    @Test
    void testTravelTime_increasesWithCongestion() {
        double emptyTime = edge.getActualTravelTime();

        TrafficFlow flow = new TrafficFlow("F1", nodeA, nodeB, 50);
        edge.addVehicle(flow);

        double congestedTime = edge.getActualTravelTime();
        assertTrue(congestedTime > emptyTime,
            "Travel time should be longer under congestion");
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
