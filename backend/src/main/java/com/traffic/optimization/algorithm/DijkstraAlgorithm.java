package com.traffic.optimization.algorithm;

import com.traffic.optimization.model.Edge;
import com.traffic.optimization.model.Graph;
import com.traffic.optimization.model.Node;

import java.util.*;

/**
 * 路径规划算法实现 - Dijkstra 和 A*
 *
 * @author Chengkun Liao, Mingjie Shen
 */
public class DijkstraAlgorithm {

    /**
     * 计算从起点到终点的最短路径 (Dijkstra)
     *
     * @param graph 道路网络图
     * @param start 起点节点
     * @param end   终点节点
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
     * A* 算法 - 使用启发式函数加速寻路
     * 启发式函数使用节点间的欧几里得距离
     *
     * @param graph 道路网络图
     * @param start 起点节点
     * @param end   终点节点
     * @return 最短路径的节点列表
     */
    public static List<Node> findShortestPathAStar(Graph graph, Node start, Node end) {
        if (start == null || end == null) {
            return null;
        }

        if (start.equals(end)) {
            return Collections.singletonList(start);
        }

        // g(n): 从起点到节点n的实际代价
        Map<Node, Double> gScore = new HashMap<>();
        // f(n) = g(n) + h(n): 估计的总代价
        Map<Node, Double> fScore = new HashMap<>();
        Map<Node, Node> predecessors = new HashMap<>();
        Set<Node> visited = new HashSet<>();

        // 优先队列按 f(n) 排序
        PriorityQueue<NodeDistance> openSet = new PriorityQueue<>(
            Comparator.comparingDouble(nd -> nd.distance)
        );

        for (Node node : graph.getAllNodes()) {
            gScore.put(node, Double.POSITIVE_INFINITY);
            fScore.put(node, Double.POSITIVE_INFINITY);
        }

        gScore.put(start, 0.0);
        fScore.put(start, heuristic(start, end));
        openSet.offer(new NodeDistance(start, fScore.get(start)));

        while (!openSet.isEmpty()) {
            NodeDistance current = openSet.poll();
            Node currentNode = current.node;

            if (visited.contains(currentNode)) {
                continue;
            }

            if (currentNode.equals(end)) {
                return reconstructPath(predecessors, start, end);
            }

            visited.add(currentNode);

            for (Edge edge : currentNode.getOutgoingEdges()) {
                Node neighbor = edge.getToNode();

                if (visited.contains(neighbor)) {
                    continue;
                }

                double tentativeG = gScore.get(currentNode) + edge.getDistance();

                if (tentativeG < gScore.get(neighbor)) {
                    predecessors.put(neighbor, currentNode);
                    gScore.put(neighbor, tentativeG);
                    double f = tentativeG + heuristic(neighbor, end);
                    fScore.put(neighbor, f);
                    openSet.offer(new NodeDistance(neighbor, f));
                }
            }
        }

        return null; // 无法到达
    }

    /**
     * 考虑实时拥堵的最短路径 (Dijkstra with dynamic weights)
     * 边权重 = 实际通行时间（考虑拥堵），而非静态距离
     *
     * @param graph 道路网络图
     * @param start 起点节点
     * @param end   终点节点
     * @return 最快路径的节点列表
     */
    public static List<Node> findFastestPath(Graph graph, Node start, Node end) {
        if (start == null || end == null) {
            return null;
        }

        if (start.equals(end)) {
            return Collections.singletonList(start);
        }

        Map<Node, Double> travelTimes = new HashMap<>();
        Map<Node, Node> predecessors = new HashMap<>();
        Set<Node> visited = new HashSet<>();

        PriorityQueue<NodeDistance> queue = new PriorityQueue<>(
            Comparator.comparingDouble(nd -> nd.distance)
        );

        for (Node node : graph.getAllNodes()) {
            travelTimes.put(node, Double.POSITIVE_INFINITY);
        }
        travelTimes.put(start, 0.0);
        queue.offer(new NodeDistance(start, 0.0));

        while (!queue.isEmpty()) {
            NodeDistance current = queue.poll();
            Node currentNode = current.node;

            if (visited.contains(currentNode)) {
                continue;
            }

            visited.add(currentNode);

            if (currentNode.equals(end)) {
                break;
            }

            for (Edge edge : currentNode.getOutgoingEdges()) {
                Node neighbor = edge.getToNode();

                if (visited.contains(neighbor)) {
                    continue;
                }

                // 使用实际通行时间（考虑拥堵）作为权重
                double newTime = travelTimes.get(currentNode) + edge.getActualTravelTime();

                if (newTime < travelTimes.get(neighbor)) {
                    travelTimes.put(neighbor, newTime);
                    predecessors.put(neighbor, currentNode);
                    queue.offer(new NodeDistance(neighbor, newTime));
                }
            }
        }

        return reconstructPath(predecessors, start, end);
    }

    /**
     * 计算从起点到所有其他节点的最短路径
     */
    public static Map<Node, List<Node>> findAllShortestPaths(Graph graph, Node start) {
        Map<Node, List<Node>> allPaths = new HashMap<>();

        Map<Node, Double> distances = new HashMap<>();
        Map<Node, Node> predecessors = new HashMap<>();
        Set<Node> visited = new HashSet<>();

        PriorityQueue<NodeDistance> queue = new PriorityQueue<>(
            Comparator.comparingDouble(nd -> nd.distance)
        );

        for (Node node : graph.getAllNodes()) {
            distances.put(node, Double.POSITIVE_INFINITY);
        }
        distances.put(start, 0.0);
        queue.offer(new NodeDistance(start, 0.0));

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

        for (Node node : graph.getAllNodes()) {
            if (!node.equals(start) && distances.get(node) != Double.POSITIVE_INFINITY) {
                allPaths.put(node, reconstructPath(predecessors, start, node));
            }
        }

        return allPaths;
    }

    /**
     * 重建从起点到终点的路径
     * 优化：使用 ArrayList + reverse 代替 LinkedList.addFirst() O(n²) -> O(n)
     */
    private static List<Node> reconstructPath(Map<Node, Node> predecessors, Node start, Node end) {
        if (!predecessors.containsKey(end) && !start.equals(end)) {
            return null; // 无法到达
        }

        ArrayList<Node> path = new ArrayList<>();
        Node current = end;

        while (current != null) {
            path.add(current);
            if (current.equals(start)) {
                break;
            }
            current = predecessors.get(current);
        }

        // 如果路径无效
        if (path.isEmpty() || !path.get(path.size() - 1).equals(start)) {
            return null;
        }

        // O(n) 反转，总体 O(n) 代替原来的 O(n²)
        Collections.reverse(path);
        return path;
    }

    /**
     * A* 启发式函数 - 欧几里得距离
     */
    private static double heuristic(Node a, Node b) {
        double dx = a.getX() - b.getX();
        double dy = a.getY() - b.getY();
        return Math.sqrt(dx * dx + dy * dy);
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
     * 检查图的连通性（从起点能否到达终点）
     */
    public static boolean isReachable(Graph graph, Node start, Node end) {
        if (start == null || end == null) {
            return false;
        }
        if (start.equals(end)) {
            return true;
        }

        Set<Node> visited = new HashSet<>();
        Queue<Node> queue = new LinkedList<>();
        queue.add(start);
        visited.add(start);

        while (!queue.isEmpty()) {
            Node current = queue.poll();
            for (Edge edge : current.getOutgoingEdges()) {
                Node neighbor = edge.getToNode();
                if (neighbor.equals(end)) {
                    return true;
                }
                if (!visited.contains(neighbor)) {
                    visited.add(neighbor);
                    queue.add(neighbor);
                }
            }
        }
        return false;
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
