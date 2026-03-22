package com.traffic.optimization.model;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import lombok.Data;
import java.util.ArrayList;
import java.util.List;

/**
 * Node class - represents an intersection/junction
 *
 * @author Chengkun Liao, Mingjie Shen
 */
@Data
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
@JsonIgnoreProperties({"trafficLight", "incomingEdges", "outgoingEdges"})
public class Node {
    /**
     * Node ID (unique identifier)
     */
    private String id;

    /**
     * Node name
     */
    private String name;

    /**
     * Node type (intersection or boundary)
     */
    private NodeType type;

    /**
     * X coordinate (for visualization)
     */
    private double x;

    /**
     * Y coordinate (for visualization)
     */
    private double y;

    /**
     * Outgoing edge list (roads departing from this node)
     */
    private List<Edge> outgoingEdges;

    /**
     * Incoming edge list (roads arriving at this node)
     */
    private List<Edge> incomingEdges;

    /**
     * Traffic light controller (only for intersection nodes)
     */
    private TrafficLight trafficLight;

    /**
     * Constructor
     */
    public Node(String id, String name, NodeType type, double x, double y) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.x = x;
        this.y = y;
        this.outgoingEdges = new ArrayList<>();
        this.incomingEdges = new ArrayList<>();

        // If intersection node, initialize traffic light
        if (type == NodeType.INTERSECTION) {
            this.trafficLight = new TrafficLight(id);
        }
    }

    /**
     * Add outgoing edge
     */
    public void addOutgoingEdge(Edge edge) {
        this.outgoingEdges.add(edge);
    }

    /**
     * Add incoming edge
     */
    public void addIncomingEdge(Edge edge) {
        this.incomingEdges.add(edge);
    }

    /**
     * Get edge to target node
     */
    public Edge getEdgeTo(Node targetNode) {
        for (Edge edge : outgoingEdges) {
            if (edge.getToNode().equals(targetNode)) {
                return edge;
            }
        }
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Node node = (Node) o;
        return id.equals(node.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "Node{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", type=" + type +
                ", position=(" + x + ", " + y + ")" +
                '}';
    }
}
