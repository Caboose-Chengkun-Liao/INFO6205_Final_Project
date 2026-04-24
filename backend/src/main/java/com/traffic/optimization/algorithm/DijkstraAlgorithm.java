package com.traffic.optimization.algorithm;

import com.traffic.optimization.model.Edge;
import com.traffic.optimization.model.Graph;
import com.traffic.optimization.model.Node;

import java.util.*;

/**
 * Path-finding algorithm implementations - Dijkstra and A*
 *
 * @author Chengkun Liao, Mingjie Shen
 */
public class DijkstraAlgorithm {

    /**
     * Find the shortest path from start to end (Dijkstra)
     *
     * @param graph road network graph
     * @param start source node
     * @param end   destination node
     * @return list of nodes on the shortest path, or null if no path exists
     */
    public static List<Node> findShortestPath(Graph graph, Node start, Node end) {
        if (start == null || end == null) {
            return null;
        }

        if (start.equals(end)) {
            return Collections.singletonList(start);
        }

        // Distance map: shortest distance from the source to each node
        Map<Node, Double> distances = new HashMap<>();

        // Predecessor map: used to reconstruct the path
        Map<Node, Node> predecessors = new HashMap<>();

        // Set of visited nodes
        Set<Node> visited = new HashSet<>();

        // Priority queue sorted by distance
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

            // Skip already-visited nodes
            if (visited.contains(currentNode)) {
                continue;
            }

            visited.add(currentNode);

            // Early termination when the destination is reached
            if (currentNode.equals(end)) {
                break;
            }

            // Examine all neighbors
            for (Edge edge : currentNode.getOutgoingEdges()) {
                Node neighbor = edge.getToNode();

                if (visited.contains(neighbor)) {
                    continue;
                }

                // Calculate the distance to the neighbor via the current node
                double newDistance = distances.get(currentNode) + edge.getDistance();

                // Update if a shorter path is found
                if (newDistance < distances.get(neighbor)) {
                    distances.put(neighbor, newDistance);
                    predecessors.put(neighbor, currentNode);
                    queue.offer(new NodeDistance(neighbor, newDistance));
                }
            }
        }

        // Reconstruct the path
        return reconstructPath(predecessors, start, end);
    }

    /**
     * A* algorithm - uses a heuristic to accelerate path finding.
     * The heuristic function is the Euclidean distance between nodes.
     *
     * @param graph road network graph
     * @param start source node
     * @param end   destination node
     * @return list of nodes on the shortest path
     */
    public static List<Node> findShortestPathAStar(Graph graph, Node start, Node end) {
        if (start == null || end == null) {
            return null;
        }

        if (start.equals(end)) {
            return Collections.singletonList(start);
        }

        // g(n): actual cost from start to node n
        Map<Node, Double> gScore = new HashMap<>();
        // f(n) = g(n) + h(n): estimated total cost
        Map<Node, Double> fScore = new HashMap<>();
        Map<Node, Node> predecessors = new HashMap<>();
        Set<Node> visited = new HashSet<>();

        // Priority queue sorted by f(n)
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

        return null; // destination is unreachable
    }

    /**
     * Congestion-aware shortest path (Dijkstra with dynamic weights).
     * Edge weight = actual travel time (accounting for congestion) rather than static distance.
     *
     * @param graph road network graph
     * @param start source node
     * @param end   destination node
     * @return list of nodes on the fastest path
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

                // Use actual travel time (accounting for congestion) as edge weight
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
     * Find shortest paths from the start node to all other nodes
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
     * Reconstruct the path from start to end.
     * Optimized: uses ArrayList + reverse instead of LinkedList.addFirst() - O(n) instead of O(n^2)
     */
    private static List<Node> reconstructPath(Map<Node, Node> predecessors, Node start, Node end) {
        if (!predecessors.containsKey(end) && !start.equals(end)) {
            return null; // destination unreachable
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

        // Path is invalid if it does not reach the start
        if (path.isEmpty() || !path.get(path.size() - 1).equals(start)) {
            return null;
        }

        // O(n) reverse; overall O(n) instead of the original O(n^2)
        Collections.reverse(path);
        return path;
    }

    /**
     * A* heuristic - scaled Euclidean distance.
     *
     * Node coordinates are not in km; the minimum km/euclidean ratio across all
     * edges in the Arlington graph is ~0.108 (bN4→n60: 0.5km / 4.635 units).
     * Using SCALE ≤ 0.108 guarantees h(n) ≤ actual remaining distance (admissible),
     * so A* is guaranteed to find the same shortest path as Dijkstra.
     */
    private static final double HEURISTIC_SCALE = 0.10;

    private static double heuristic(Node a, Node b) {
        double dx = a.getX() - b.getX();
        double dy = a.getY() - b.getY();
        return Math.sqrt(dx * dx + dy * dy) * HEURISTIC_SCALE;
    }

    /**
     * Calculate the total distance of a path
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
     * Check graph connectivity (whether the destination is reachable from the source)
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
     * Helper class: pairing of a node with a distance value
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
