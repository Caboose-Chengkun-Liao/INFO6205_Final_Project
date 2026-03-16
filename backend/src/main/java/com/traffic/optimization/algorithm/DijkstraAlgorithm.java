package com.traffic.optimization.algorithm;

import com.traffic.optimization.model.Edge;
import com.traffic.optimization.model.Graph;
import com.traffic.optimization.model.Node;

import java.util.*;

/**
 * Dijkstra最短路径算法实现
 *
 * @author Chengkun Liao, Mingjie Shen
 */
public class DijkstraAlgorithm {

    /**
     * 计算从起点到终点的最短路径
     *
     * @param graph 道路网络图
     * @param start 起点节点
     * @param end 终点节点
     * @return 最短路径的节点列表，如果不存在路径则返回null
     */
    public static List<Node> findShortestPath(Graph graph, Node start, Node end) {
        if (start == null || end == null) {
            return null;
        }

        if (start.equals(end)) {
            return Collections.singletonList(start);
        }

        // 距离映射：每个节点到起点的最短距离
        Map<Node, Double> distances = new HashMap<>();

        // 前驱节点映射：用于重建路径
        Map<Node, Node> predecessors = new HashMap<>();

        // 已访问节点集合
        Set<Node> visited = new HashSet<>();

        // 优先队列：按距离排序
        PriorityQueue<NodeDistance> queue = new PriorityQueue<>(
            Comparator.comparingDouble(nd -> nd.distance)
        );

        // 初始化
        for (Node node : graph.getAllNodes()) {
            distances.put(node, Double.POSITIVE_INFINITY);
        }
        distances.put(start, 0.0);
        queue.offer(new NodeDistance(start, 0.0));

        // Dijkstra主循环
        while (!queue.isEmpty()) {
            NodeDistance current = queue.poll();
            Node currentNode = current.node;

            // 如果已访问过，跳过
            if (visited.contains(currentNode)) {
                continue;
            }

            visited.add(currentNode);

            // 如果到达目标节点，提前终止
            if (currentNode.equals(end)) {
                break;
            }

            // 检查所有邻居
            for (Edge edge : currentNode.getOutgoingEdges()) {
                Node neighbor = edge.getToNode();

                if (visited.contains(neighbor)) {
                    continue;
                }

                // 计算通过当前节点到邻居的距离
                double newDistance = distances.get(currentNode) + edge.getDistance();

                // 如果找到更短的路径，更新
                if (newDistance < distances.get(neighbor)) {
                    distances.put(neighbor, newDistance);
                    predecessors.put(neighbor, currentNode);
                    queue.offer(new NodeDistance(neighbor, newDistance));
                }
            }
        }

        // 重建路径
        return reconstructPath(predecessors, start, end);
    }

    /**
     * 计算从起点到所有其他节点的最短路径
     *
     * @param graph 道路网络图
     * @param start 起点节点
     * @return 从起点到所有节点的最短路径映射
     */
    public static Map<Node, List<Node>> findAllShortestPaths(Graph graph, Node start) {
        Map<Node, List<Node>> allPaths = new HashMap<>();

        // 距离映射
        Map<Node, Double> distances = new HashMap<>();

        // 前驱节点映射
        Map<Node, Node> predecessors = new HashMap<>();

        // 已访问节点集合
        Set<Node> visited = new HashSet<>();

        // 优先队列
        PriorityQueue<NodeDistance> queue = new PriorityQueue<>(
            Comparator.comparingDouble(nd -> nd.distance)
        );

        // 初始化
        for (Node node : graph.getAllNodes()) {
            distances.put(node, Double.POSITIVE_INFINITY);
        }
        distances.put(start, 0.0);
        queue.offer(new NodeDistance(start, 0.0));

        // Dijkstra主循环
        while (!queue.isEmpty()) {
            NodeDistance current = queue.poll();
            Node currentNode = current.node;

            if (visited.contains(currentNode)) {
                continue;
            }

            visited.add(currentNode);

            for (Edge edge : currentNode.getOutgoingEdges()) {
                Node neighbor = edge.getToNode();

                if (visited.contains(neighbor)) {
                    continue;
                }

                double newDistance = distances.get(currentNode) + edge.getDistance();

                if (newDistance < distances.get(neighbor)) {
                    distances.put(neighbor, newDistance);
                    predecessors.put(neighbor, currentNode);
                    queue.offer(new NodeDistance(neighbor, newDistance));
                }
            }
        }

        // 为所有可达节点重建路径
        for (Node node : graph.getAllNodes()) {
            if (!node.equals(start) && distances.get(node) != Double.POSITIVE_INFINITY) {
                allPaths.put(node, reconstructPath(predecessors, start, node));
            }
        }

        return allPaths;
    }

    /**
     * 重建从起点到终点的路径
     */
    private static List<Node> reconstructPath(Map<Node, Node> predecessors, Node start, Node end) {
        if (!predecessors.containsKey(end) && !start.equals(end)) {
            return null; // 无法到达
        }

        LinkedList<Node> path = new LinkedList<>();
        Node current = end;

        while (current != null) {
            path.addFirst(current);
            if (current.equals(start)) {
                break;
            }
            current = predecessors.get(current);
        }

        return path.isEmpty() || !path.getFirst().equals(start) ? null : path;
    }

    /**
     * 计算路径的总距离
     */
    public static double calculatePathDistance(List<Node> path) {
        if (path == null || path.size() < 2) {
            return 0;
        }

        double totalDistance = 0;
        for (int i = 0; i < path.size() - 1; i++) {
            Node from = path.get(i);
            Node to = path.get(i + 1);
            Edge edge = from.getEdgeTo(to);
            if (edge != null) {
                totalDistance += edge.getDistance();
            }
        }

        return totalDistance;
    }

    /**
     * 辅助类：节点和距离的配对
     */
    private static class NodeDistance {
        Node node;
        double distance;

        NodeDistance(Node node, double distance) {
            this.node = node;
            this.distance = distance;
        }
    }
}
