package com.traffic.optimization.model;

import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Graph class - represents the entire road network
 *
 * @author Chengkun Liao, Mingjie Shen
 */
@Data
public class Graph {

    private static final Logger log = LoggerFactory.getLogger(Graph.class);
    /**
     * Map of all nodes (ID -> Node)
     */
    private Map<String, Node> nodes;

    /**
     * List of all edges
     */
    private List<Edge> edges;

    /**
     * List of boundary nodes (entry and exit points for traffic flows)
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
     * Add a node
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
     * Add an edge
     */
    public void addEdge(Edge edge) {
        edges.add(edge);

        // Update outgoing and incoming edge lists on the connected nodes
        edge.getFromNode().addOutgoingEdge(edge);
        edge.getToNode().addIncomingEdge(edge);
    }

    /**
     * Add a bidirectional road
     */
    public void addBidirectionalEdge(String id1, String id2, Node node1, Node node2, double distance) {
        Edge edge1 = new Edge(id1, node1, node2, distance);
        Edge edge2 = new Edge(id2, node2, node1, distance);

        addEdge(edge1);
        addEdge(edge2);
    }

    /**
     * Get a node by ID
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
     * Get the neighbors of a node
     */
    public List<Node> getNeighbors(Node node) {
        List<Node> neighbors = new ArrayList<>();
        for (Edge edge : node.getOutgoingEdges()) {
            neighbors.add(edge.getToNode());
        }
        return neighbors;
    }

    /**
     * Get the edge between two nodes
     */
    public Edge getEdge(Node from, Node to) {
        return from.getEdgeTo(to);
    }

    /**
     * Get the number of nodes
     */
    public int getNodeCount() {
        return nodes.size();
    }

    /**
     * Get the number of edges
     */
    public int getEdgeCount() {
        return edges.size();
    }

    /**
     * Check whether the graph is empty
     */
    public boolean isEmpty() {
        return nodes.isEmpty();
    }

    /**
     * Clear the graph
     */
    public void clear() {
        nodes.clear();
        edges.clear();
        boundaryNodes.clear();
        intersectionNodes.clear();
    }

    /**
     * Deep copy the entire graph — new Node, Edge, TrafficLight objects.
     * Used by ComparisonController to run parallel independent simulations.
     */
    public Graph deepCopy() {
        Graph copy = new Graph();

        // 1. Copy all nodes (new objects, same coordinates)
        Map<String, Node> nodeMapping = new HashMap<>();
        for (Node original : nodes.values()) {
            Node cloned = new Node(original.getId(), original.getName(),
                    original.getType(), original.getX(), original.getY());
            copy.addNode(cloned);
            nodeMapping.put(original.getId(), cloned);
        }

        // 2. Copy all edges using cloned nodes
        for (Edge original : edges) {
            Node fromCloned = nodeMapping.get(original.getFromNode().getId());
            Node toCloned = nodeMapping.get(original.getToNode().getId());
            if (fromCloned != null && toCloned != null) {
                Edge cloned = new Edge(original.getId(), fromCloned, toCloned,
                        original.getDistance(), original.getCapacityPerKm(), original.getSpeedLimit());
                copy.addEdge(cloned);
            }
        }

        return copy;
    }

    /**
     * Print road network statistics
     */
    public void printStatistics() {
        log.info("Road network statistics - nodes: {}, intersections: {}, boundaries: {}, roads: {}",
            getNodeCount(), intersectionNodes.size(), boundaryNodes.size(), getEdgeCount());
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
