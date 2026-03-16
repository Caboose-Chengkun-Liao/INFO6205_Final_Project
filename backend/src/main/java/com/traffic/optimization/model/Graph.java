package com.traffic.optimization.model;

import lombok.Data;
import java.util.*;

/**
 * 图类 - 代表整个道路网络
 *
 * @author Chengkun Liao, Mingjie Shen
 */
@Data
public class Graph {
    /**
     * 所有节点的映射（ID -> Node）
     */
    private Map<String, Node> nodes;

    /**
     * 所有边的列表
     */
    private List<Edge> edges;

    /**
     * 边界节点列表（交通流的起点和终点）
     */
    private List<Node> boundaryNodes;

    /**
     * 路口节点列表（有信号灯的节点）
     */
    private List<Node> intersectionNodes;

    /**
     * 构造函数
     */
    public Graph() {
        this.nodes = new HashMap<>();
        this.edges = new ArrayList<>();
        this.boundaryNodes = new ArrayList<>();
        this.intersectionNodes = new ArrayList<>();
    }

    /**
     * 添加节点
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
     * 添加边
     */
    public void addEdge(Edge edge) {
        edges.add(edge);

        // 更新节点的出边和入边
        edge.getFromNode().addOutgoingEdge(edge);
        edge.getToNode().addIncomingEdge(edge);
    }

    /**
     * 添加双向道路
     */
    public void addBidirectionalEdge(String id1, String id2, Node node1, Node node2, double distance) {
        Edge edge1 = new Edge(id1, node1, node2, distance);
        Edge edge2 = new Edge(id2, node2, node1, distance);

        addEdge(edge1);
        addEdge(edge2);
    }

    /**
     * 根据ID获取节点
     */
    public Node getNode(String id) {
        return nodes.get(id);
    }

    /**
     * 获取所有节点
     */
    public Collection<Node> getAllNodes() {
        return nodes.values();
    }

    /**
     * 获取节点的邻居
     */
    public List<Node> getNeighbors(Node node) {
        List<Node> neighbors = new ArrayList<>();
        for (Edge edge : node.getOutgoingEdges()) {
            neighbors.add(edge.getToNode());
        }
        return neighbors;
    }

    /**
     * 获取两个节点之间的边
     */
    public Edge getEdge(Node from, Node to) {
        return from.getEdgeTo(to);
    }

    /**
     * 获取节点数量
     */
    public int getNodeCount() {
        return nodes.size();
    }

    /**
     * 获取边数量
     */
    public int getEdgeCount() {
        return edges.size();
    }

    /**
     * 检查图是否为空
     */
    public boolean isEmpty() {
        return nodes.isEmpty();
    }

    /**
     * 清空图
     */
    public void clear() {
        nodes.clear();
        edges.clear();
        boundaryNodes.clear();
        intersectionNodes.clear();
    }

    /**
     * 打印图的统计信息
     */
    public void printStatistics() {
        System.out.println("=== 道路网络统计 ===");
        System.out.println("总节点数: " + getNodeCount());
        System.out.println("路口节点: " + intersectionNodes.size());
        System.out.println("边界节点: " + boundaryNodes.size());
        System.out.println("总道路数: " + getEdgeCount());
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
