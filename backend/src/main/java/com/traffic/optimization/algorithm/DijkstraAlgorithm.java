package com.traffic.optimization.algorithm;

import com.traffic.optimization.model.Edge;
import com.traffic.optimization.model.Graph;
import com.traffic.optimization.model.Node;

import java.util.*;

/**
 * Dijkstra shortest path algorithm implementation
 *
 * @author Chengkun Liao, Mingjie Shen
 */
public class DijkstraAlgorithm {

    /**
     * Calculate shortest path from start to end
     *
     * @param graph road network graph
     * @param start start node
     * @param end end node
     * @return list of nodes on shortest path, or null if no path exists
     */
    public static List<Node> findShortestPath(Graph graph, Node start, Node end) {
        if (start == null || end == null) {
            return null;
        }

        if (start.equals(end)) {
            return Collections.singletonList(start);
        }

        // Distance map: shortest distance from each node to start
        Map<Node, Double> distances = new HashMap<>();

        // Predecessor map: used to reconstruct path
        Map<Node, Node> predecessors = new HashMap<>();

        // Visited node set
        Set<Node> visited = new HashSet<>();

        // Priority queue: sorted by distance
        PriorityQueue<NodeDistance> queue = new PriorityQueue<>(
            Comparator.comparingDouble(nd -> nd.distance)
        );

        // Initialize
        for (Node node : graph.getAllNodes()) {
            distances.put(node, Double.POSITIVE_INFINITY);
        }
        distances.put(start, 0.0);
        queue.offer(new NodeDistance(start, 0.0));

        // Dijkstra main loop
        while (!queue.isEmpty()) {
            NodeDistance current = queue.poll();
            Node currentNode = current.node;

            // Skip if already visited
            if (visited.contains(currentNode)) {
                continue;
            }

            visited.add(currentNode);

            // Early termination if target node is reached
            if (currentNode.equals(end)) {
                break;
            }

            // Check all neighbors
            for (Edge edge : currentNode.getOutgoingEdges()) {
                Node neighbor = edge.getToNode();

                if (visited.contains(neighbor)) {
                    continue;
                }

                // Calculate distance to neighbor through current node
                double newDistance = distances.get(currentNode) + edge.getDistance();

                // Update if shorter path found
                if (newDistance < distances.get(neighbor)) {
                    distances.put(neighbor, newDistance);
                    predecessors.put(neighbor, currentNode);
                    queue.offer(new NodeDistance(neighbor, newDistance));
                }
            }
        }

        // Reconstruct path
        return reconstructPath(predecessors, start, end);
    }

    /**
     * Calculate shortest paths from start to all other nodes
     *
     * @param graph road network graph
     * @param start start node
     * @return map of shortest paths from start to all nodes
     */
    public static Map<Node, List<Node>> findAllShortestPaths(Graph graph, Node start) {
        Map<Node, List<Node>> allPaths = new HashMap<>();

        // Distance map
        Map<Node, Double> distances = new HashMap<>();

        // Predecessor map
        Map<Node, Node> predecessors = new HashMap<>();

        // Visited node set
        Set<Node> visited = new HashSet<>();

        // Priority queue
        PriorityQueue<NodeDistance> queue = new PriorityQueue<>(
            Comparator.comparingDouble(nd -> nd.distance)
        );

        // Initialize
        for (Node node : graph.getAllNodes()) {
            distances.put(node, Double.POSITIVE_INFINITY);
        }
        distances.put(start, 0.0);
        queue.offer(new NodeDistance(start, 0.0));

        // Dijkstra main loop
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

        // Reconstruct paths for all reachable nodes
        for (Node node : graph.getAllNodes()) {
            if (!node.equals(start) && distances.get(node) != Double.POSITIVE_INFINITY) {
                allPaths.put(node, reconstructPath(predecessors, start, node));
            }
        }

        return allPaths;
    }

    /**
     * Reconstruct path from start to end
     */
    private static List<Node> reconstructPath(Map<Node, Node> predecessors, Node start, Node end) {
        if (!predecessors.containsKey(end) && !start.equals(end)) {
            return null; // unreachable
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
     * Calculate total distance of a path
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
     * Helper class: pairing of node and distance
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
