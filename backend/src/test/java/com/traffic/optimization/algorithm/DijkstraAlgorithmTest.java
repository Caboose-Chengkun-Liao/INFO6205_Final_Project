package com.traffic.optimization.algorithm;

import com.traffic.optimization.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Dijkstra算法单元测试
 */
class DijkstraAlgorithmTest {

    private Graph graph;
    private Node nodeA, nodeB, nodeC, nodeD;

    @BeforeEach
    void setUp() {
        graph = new Graph();

        // 创建节点
        nodeA = new Node("A", "Node A", NodeType.BOUNDARY, 0, 0);
        nodeB = new Node("B", "Node B", NodeType.INTERSECTION, 1, 0);
        nodeC = new Node("C", "Node C", NodeType.INTERSECTION, 1, 1);
        nodeD = new Node("D", "Node D", NodeType.BOUNDARY, 2, 1);

        graph.addNode(nodeA);
        graph.addNode(nodeB);
        graph.addNode(nodeC);
        graph.addNode(nodeD);

        // 创建边: A -> B (5km), B -> C (3km), C -> D (2km), A -> C (10km)
        graph.addEdge(new Edge("AB", nodeA, nodeB, 5.0));
        graph.addEdge(new Edge("BC", nodeB, nodeC, 3.0));
        graph.addEdge(new Edge("CD", nodeC, nodeD, 2.0));
        graph.addEdge(new Edge("AC", nodeA, nodeC, 10.0));
    }

    @Test
    void testFindShortestPath() {
        List<Node> path = DijkstraAlgorithm.findShortestPath(graph, nodeA, nodeD);

        assertNotNull(path);
        assertEquals(4, path.size());
        assertEquals(nodeA, path.get(0));
        assertEquals(nodeB, path.get(1));
        assertEquals(nodeC, path.get(2));
        assertEquals(nodeD, path.get(3));
    }

    @Test
    void testCalculatePathDistance() {
        List<Node> path = DijkstraAlgorithm.findShortestPath(graph, nodeA, nodeD);
        double distance = DijkstraAlgorithm.calculatePathDistance(path);

        assertEquals(10.0, distance, 0.001); // 5 + 3 + 2 = 10
    }

    @Test
    void testSameStartAndEnd() {
        List<Node> path = DijkstraAlgorithm.findShortestPath(graph, nodeA, nodeA);

        assertNotNull(path);
        assertEquals(1, path.size());
        assertEquals(nodeA, path.get(0));
    }

    @Test
    void testNoPath() {
        Node isolatedNode = new Node("E", "Node E", NodeType.BOUNDARY, 5, 5);
        graph.addNode(isolatedNode);

        List<Node> path = DijkstraAlgorithm.findShortestPath(graph, nodeA, isolatedNode);

        assertNull(path);
    }
}
