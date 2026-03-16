package com.traffic.optimization.model;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import lombok.Data;
import java.util.ArrayList;
import java.util.List;

/**
 * 节点类 - 代表十字路口/交叉点
 *
 * @author Chengkun Liao, Mingjie Shen
 */
@Data
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
@JsonIgnoreProperties({"trafficLight", "incomingEdges", "outgoingEdges"})
public class Node {
    /**
     * 节点ID（唯一标识符）
     */
    private String id;

    /**
     * 节点名称
     */
    private String name;

    /**
     * 节点类型（路口或边界）
     */
    private NodeType type;

    /**
     * X坐标（用于可视化）
     */
    private double x;

    /**
     * Y坐标（用于可视化）
     */
    private double y;

    /**
     * 出边列表（从该节点出发的道路）
     */
    private List<Edge> outgoingEdges;

    /**
     * 入边列表（到达该节点的道路）
     */
    private List<Edge> incomingEdges;

    /**
     * 信号灯控制器（仅用于路口节点）
     */
    private TrafficLight trafficLight;

    /**
     * 构造函数
     */
    public Node(String id, String name, NodeType type, double x, double y) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.x = x;
        this.y = y;
        this.outgoingEdges = new ArrayList<>();
        this.incomingEdges = new ArrayList<>();

        // 如果是路口节点，初始化信号灯
        if (type == NodeType.INTERSECTION) {
            this.trafficLight = new TrafficLight(id);
        }
    }

    /**
     * 添加出边
     */
    public void addOutgoingEdge(Edge edge) {
        this.outgoingEdges.add(edge);
    }

    /**
     * 添加入边
     */
    public void addIncomingEdge(Edge edge) {
        this.incomingEdges.add(edge);
    }

    /**
     * 获取到指定节点的边
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
