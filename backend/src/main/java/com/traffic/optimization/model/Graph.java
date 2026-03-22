package com.traffic.optimization.model;

import lombok.Data;
import java.util.*;

/**
 * Graph class - represents the entire road network
 *
 * @author Chengkun Liao, Mingjie Shen
 */
@Data
public class Graph {
    /**
     * Map of all nodes (ID -> Node)
     */
    private Map<String, Node> nodes;

    /**
     * List of all edges
     */
    private List<Edge> edges;

    /**
     * List of boundary nodes (start and end points of traffic flows)
     */
    private List<Node> boundaryNodes;

    /**
     * List of intersection nodes (nodes with traffic lights)
     */
    private List<Node> intersectionNodes;

    /**
     * Constructor
     */
    public Graph() {
        this.nodes = new HashMap<>();
        this.edges = new ArrayList<>();
        this.boundaryNodes = new ArrayList<>();
        this.intersectionNodes = new ArrayList<>();
    }

    /**
     * Add node
     */
    public void addNode(Node node) {
        nodes.put(node.getId(), node);

        if (node.getType() == NodeType.BOUNDARY) {
            boundaryNodes.add(node);
        } else if (node.getType() == NodeType.INTERSECTION) {
            intersectionNodes.add(node);
        }
    }

    /**
     * Add edge
     */
    public void addEdge(Edge edge) {
        edges.add(edge);

        // Update outgoing and incoming edges of nodes
        edge.getFromNode().addOutgoingEdge(edge);
        edge.getToNode().addIncomingEdge(edge);
    }

    /**
     * Add bidirectional road
     */
    public void addBidirectionalEdge(String id1, String id2, Node node1, Node node2, double distance) {
        Edge edge1 = new Edge(id1, node1, node2, distance);
        Edge edge2 = new Edge(id2, node2, node1, distance);

        addEdge(edge1);
        addEdge(edge2);
    }

    /**
     * Get node by ID
     */
    public Node getNode(String id) {
        return nodes.get(id);
    }

    /**
     * Get all nodes
     */
    public Collection<Node> getAllNodes() {
        return nodes.values();
    }

    /**
     * Get neighbors of a node
     */
    public List<Node> getNeighbors(Node node) {
        List<Node> neighbors = new ArrayList<>();
        for (Edge edge : node.getOutgoingEdges()) {
            neighbors.add(edge.getToNode());
        }
        return neighbors;
    }

    /**
     * Get edge between two nodes
     */
    public Edge getEdge(Node from, Node to) {
        return from.getEdgeTo(to);
    }

    /**
     * Get node count
     */
    public int getNodeCount() {
        return nodes.size();
    }

    /**
     * Get edge count
     */
    public int getEdgeCount() {
        return edges.size();
    }

    /**
     * Check if graph is empty
     */
    public boolean isEmpty() {
        return nodes.isEmpty();
    }

    /**
     * Clear graph
     */
    public void clear() {
        nodes.clear();
        edges.clear();
        boundaryNodes.clear();
        intersectionNodes.clear();
    }

    /**
     * Print graph statistics
     */
    public void printStatistics() {
        System.out.println("=== Road Network Statistics ===");
        System.out.println("Total nodes: " + getNodeCount());
        System.out.println("Intersection nodes: " + intersectionNodes.size());
        System.out.println("Boundary nodes: " + boundaryNodes.size());
        System.out.println("Total roads: " + getEdgeCount());
        System.out.println("===================");
    }

    @Override
    public String toString() {
        return "Graph{" +
                "nodes=" + nodes.size() +
                ", edges=" + edges.size() +
                ", intersections=" + intersectionNodes.size() +
                ", boundaries=" + boundaryNodes.size() +
                '}';
    }
}
