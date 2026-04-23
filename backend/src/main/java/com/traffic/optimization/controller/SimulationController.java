package com.traffic.optimization.controller;

import com.traffic.optimization.algorithm.DijkstraAlgorithm;
import com.traffic.optimization.model.Edge;
import com.traffic.optimization.model.Graph;
import com.traffic.optimization.model.Node;
import com.traffic.optimization.model.NodeType;
import com.traffic.optimization.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Simulation controller - RESTful API
 *
 * @author Chengkun Liao, Mingjie Shen
 */
@RestController
@RequestMapping("/api/simulation")
@CrossOrigin(origins = "*")
public class SimulationController {

    private static final Logger log = LoggerFactory.getLogger(SimulationController.class);

    @Autowired
    private SimulationEngine simulationEngine;

    @Autowired
    private FlowManager flowManager;

    @Autowired
    private SignalController signalController;

    @Autowired
    private EfficiencyCalculator efficiencyCalculator;

    /**
     * Initialize the simulation (using the default map)
     */
    @PostMapping("/initialize")
    public ResponseEntity<Map<String, Object>> initialize() {
        try {
            Graph graph = createDefaultGraph();
            simulationEngine.initialize(graph);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Simulation initialized");
            response.put("graph", getGraphInfo(graph));

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * Start the simulation
     */
    @PostMapping("/start")
    public ResponseEntity<Map<String, Object>> start() {
        simulationEngine.start();
        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "Simulation started",
            "state", simulationEngine.getState()
        ));
    }

    /**
     * Pause the simulation
     */
    @PostMapping("/pause")
    public ResponseEntity<Map<String, Object>> pause() {
        simulationEngine.pause();
        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "Simulation paused",
            "state", simulationEngine.getState()
        ));
    }

    /**
     * Stop the simulation
     */
    @PostMapping("/stop")
    public ResponseEntity<Map<String, Object>> stop() {
        simulationEngine.stop();
        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "Simulation stopped",
            "state", simulationEngine.getState()
        ));
    }

    /**
     * Reset the simulation
     */
    @PostMapping("/reset")
    public ResponseEntity<Map<String, Object>> reset() {
        simulationEngine.reset();
        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "Simulation reset",
            "state", simulationEngine.getState()
        ));
    }

    /**
     * Execute a single step
     */
    @PostMapping("/step")
    public ResponseEntity<Map<String, Object>> step() {
        simulationEngine.step();
        return ResponseEntity.ok(Map.of(
            "success", true,
            "currentTime", simulationEngine.getCurrentTime()
        ));
    }

    /**
     * Get simulation status
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        int activeSize = flowManager.getActiveFlowsList().size();
        int completedSize = flowManager.getCompletedFlowsList().size();

        log.debug("getStatus: activeFlows={} completedFlows={}", activeSize, completedSize);

        Map<String, Object> status = new HashMap<>();
        status.put("state", simulationEngine.getState());
        status.put("currentTime", simulationEngine.getCurrentTime());
        status.put("metrics", simulationEngine.getCurrentMetrics());
        status.put("activeFlows", activeSize);
        status.put("completedFlows", completedSize);

        return ResponseEntity.ok(status);
    }

    /**
     * Get performance metrics
     */
    @GetMapping("/metrics")
    public ResponseEntity<EfficiencyCalculator.PerformanceMetrics> getMetrics() {
        return ResponseEntity.ok(simulationEngine.getCurrentMetrics());
    }

    /**
     * Get efficiency trend
     */
    @GetMapping("/efficiency/trend")
    public ResponseEntity<List<EfficiencyCalculator.EfficiencyRecord>> getEfficiencyTrend(
            @RequestParam(defaultValue = "50") int count) {
        return ResponseEntity.ok(efficiencyCalculator.getEfficiencyTrend(count));
    }

    /**
     * Create a traffic flow
     */
    @PostMapping("/flows")
    public ResponseEntity<Map<String, Object>> createFlow(@RequestBody FlowManager.FlowRequest request) {
        try {
            // Input validation
            if (request.getEntryPoint() == null || request.getEntryPoint().isBlank()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "error", "Entry node cannot be empty"));
            }
            if (request.getDestination() == null || request.getDestination().isBlank()) {
                return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "error", "Destination node cannot be empty"));
            }
            if (request.getEntryPoint().equals(request.getDestination())) {
                return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "error", "Entry and destination cannot be the same"));
            }
            if (request.getNumberOfCars() <= 0) {
                return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "error", "Number of vehicles must be greater than 0"));
            }
            if (request.getNumberOfCars() > 200) {
                return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "error", "Number of vehicles per request cannot exceed 200"));
            }

            var flow = flowManager.createFlow(
                request.getEntryPoint(),
                request.getDestination(),
                request.getNumberOfCars()
            );

            return ResponseEntity.ok(Map.of(
                "success", true,
                "flowId", flow.getFlowId()
            ));
        } catch (Exception e) {
            log.error("Failed to create traffic flow: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * Get road network graph data (for frontend map rendering)
     */
    @GetMapping("/graph")
    public ResponseEntity<Map<String, Object>> getGraph() {
        Graph graph = simulationEngine.getGraph();
        if (graph == null) {
            return ResponseEntity.badRequest()
                .body(Map.of("success", false, "error", "Simulation not initialized"));
        }

        Map<String, Object> graphData = new HashMap<>();

        // Node data
        List<Map<String, Object>> nodes = new ArrayList<>();
        for (Node node : graph.getAllNodes()) {
            Map<String, Object> nodeData = new HashMap<>();
            nodeData.put("id", node.getId());
            nodeData.put("name", node.getName());
            nodeData.put("type", node.getType().toString());
            nodeData.put("x", node.getX());
            nodeData.put("y", node.getY());
            nodes.add(nodeData);
        }

        // Edge data
        List<Map<String, Object>> edges = new ArrayList<>();
        for (Node node : graph.getAllNodes()) {
            for (Edge edge : node.getOutgoingEdges()) {
                Map<String, Object> edgeData = new HashMap<>();
                edgeData.put("id", edge.getId());
                edgeData.put("from", edge.getFromNode().getId());
                edgeData.put("to", edge.getToNode().getId());
                edgeData.put("distance", edge.getDistance());
                edgeData.put("currentLoad", edge.getCurrentVehicleCount());
                edges.add(edgeData);
            }
        }

        graphData.put("nodes", nodes);
        graphData.put("edges", edges);

        return ResponseEntity.ok(graphData);
    }

    /**
     * Get vehicle position data (for map visualization)
     */
    @GetMapping("/vehicles")
    public ResponseEntity<List<Map<String, Object>>> getVehiclePositions() {
        List<Map<String, Object>> vehicles = new ArrayList<>();

        for (var flow : flowManager.getActiveFlowsList()) {
            if (flow.getCurrentEdge() != null) {
                Map<String, Object> vehicleData = new HashMap<>();
                vehicleData.put("flowId", flow.getFlowId());
                vehicleData.put("numberOfCars", flow.getNumberOfCars());
                vehicleData.put("state", flow.getState().toString());

                // Current position
                Edge currentEdge = flow.getCurrentEdge();
                vehicleData.put("currentEdge", currentEdge.getId());
                vehicleData.put("from", currentEdge.getFromNode().getId());
                vehicleData.put("to", currentEdge.getToNode().getId());

                // Calculate progress along the edge (0-1)
                // getActualTravelTime() returns minutes; multiply by 60 to get seconds;
                // timeOnCurrentEdge is already in seconds
                double travelTimeSeconds = currentEdge.getActualTravelTime() * 60;
                double progress = travelTimeSeconds > 0
                    ? Math.min(1.0, flow.getTimeOnCurrentEdge() / travelTimeSeconds)
                    : 0.0;
                vehicleData.put("progress", progress);

                vehicles.add(vehicleData);
            }
        }

        return ResponseEntity.ok(vehicles);
    }

    /**
     * Get all traffic light statuses
     */
    @GetMapping("/signals")
    public ResponseEntity<List<SignalController.SignalStatus>> getSignalStatuses() {
        return ResponseEntity.ok(signalController.getAllSignalStatuses());
    }

    /**
     * Set the signal optimization mode
     */
    @PostMapping("/signals/mode")
    public ResponseEntity<Map<String, Object>> setSignalMode(
            @RequestParam SignalController.OptimizationMode mode) {
        signalController.setOptimizationMode(mode);
        return ResponseEntity.ok(Map.of(
            "success", true,
            "mode", mode
        ));
    }

    /**
     * Compare three pathfinding algorithms: Dijkstra, A*, and Dynamic Routing
     */
    @GetMapping("/pathfind/compare")
    public ResponseEntity<Map<String, Object>> compareAlgorithms(
            @RequestParam String start,
            @RequestParam String end) {
        Graph graph = simulationEngine.getGraph();
        if (graph == null) {
            return ResponseEntity.badRequest()
                .body(Map.of("success", false, "error", "Simulation not initialized"));
        }

        Node startNode = graph.getNode(start);
        Node endNode = graph.getNode(end);
        if (startNode == null || endNode == null) {
            return ResponseEntity.badRequest()
                .body(Map.of("success", false, "error", "Invalid node ID"));
        }

        Map<String, Object> result = new HashMap<>();

        // Dijkstra
        long t1 = System.nanoTime();
        List<Node> dijkstraPath = DijkstraAlgorithm.findShortestPath(graph, startNode, endNode);
        long dijkstraTimeNs = System.nanoTime() - t1;
        result.put("dijkstra", buildPathResult("Dijkstra", dijkstraPath, dijkstraTimeNs));

        // A*
        long t2 = System.nanoTime();
        List<Node> aStarPath = DijkstraAlgorithm.findShortestPathAStar(graph, startNode, endNode);
        long aStarTimeNs = System.nanoTime() - t2;
        result.put("aStar", buildPathResult("A*", aStarPath, aStarTimeNs));

        // Dynamic (congestion-aware)
        long t3 = System.nanoTime();
        List<Node> dynamicPath = DijkstraAlgorithm.findFastestPath(graph, startNode, endNode);
        long dynamicTimeNs = System.nanoTime() - t3;
        result.put("dynamic", buildPathResult("Dynamic Routing", dynamicPath, dynamicTimeNs));

        result.put("success", true);
        return ResponseEntity.ok(result);
    }

    private Map<String, Object> buildPathResult(String name, List<Node> path, long timeNs) {
        Map<String, Object> r = new HashMap<>();
        r.put("name", name);
        if (path == null) {
            r.put("path", List.of());
            r.put("distance", 0.0);
            r.put("nodesInPath", 0);
            r.put("computationTimeMs", timeNs / 1_000_000.0);
            r.put("reachable", false);
            return r;
        }
        r.put("path", path.stream().map(Node::getId).collect(Collectors.toList()));
        r.put("distance", DijkstraAlgorithm.calculatePathDistance(path));
        r.put("nodesInPath", path.size());
        r.put("computationTimeMs", timeNs / 1_000_000.0);
        r.put("reachable", true);
        return r;
    }

    /**
     * Arlington road network — all intersections have max degree 4.
     * Nodes n37,n53,n75,n77,n78,n79,n81 removed; 31 artificial cross-links pruned.
     * ~93 nodes, ~248 directed edges covering Arlington County, VA.
     */
    private Graph createDefaultGraph() {
        Graph graph = new Graph();

        // ==================== Boundary Nodes (18) - Real coordinates ====================
        Node bN1 = new Node("N1", "Lee Hwy North", NodeType.BOUNDARY, -3.08, 4.77);
        Node bN2 = new Node("N2", "Washington Blvd North", NodeType.BOUNDARY, -0.21, 5.05);
        Node bN3 = new Node("N3", "Langston Blvd North", NodeType.BOUNDARY, 3.88, 6.19);
        Node bS1 = new Node("S1", "Columbia Pike South", NodeType.BOUNDARY, -3.08, -7.16);
        Node bS2 = new Node("S2", "Pentagon South", NodeType.BOUNDARY, 7.11, -5.45);
        Node bS3 = new Node("S3", "Crystal City South", NodeType.BOUNDARY, 9.44, -8.57);
        Node bE1 = new Node("E1", "Key Bridge", NodeType.BOUNDARY, 4.52, 6.33);
        Node bE2 = new Node("E2", "Memorial Bridge", NodeType.BOUNDARY, 9.12, 0.8);
        Node bE3 = new Node("E3", "14th St Bridge", NodeType.BOUNDARY, 9.8, -4.32);
        Node bW1 = new Node("W1", "Route 50 West", NodeType.BOUNDARY, -8.86, -1.53);
        Node bW2 = new Node("W2", "Lee Hwy West", NodeType.BOUNDARY, -8.86, 2.5);
        Node bW3 = new Node("W3", "Columbia Pike West", NodeType.BOUNDARY, -6.58, -6.3);
        Node bN4 = new Node("N4", "I-66 West", NodeType.BOUNDARY, -8.86, 5.05);
        Node bS4 = new Node("S4", "I-395 South", NodeType.BOUNDARY, 2.52, -8.57);
        Node bS5 = new Node("S5", "Shirlington South", NodeType.BOUNDARY, 0.92, -9.43);
        Node bE4 = new Node("E4", "GW Parkway North", NodeType.BOUNDARY, 5.93, 6.76);
        Node bE5 = new Node("E5", "Arlington Ridge Rd", NodeType.BOUNDARY, 9.34, -0.91);
        Node bW4 = new Node("W4", "George Mason Dr South", NodeType.BOUNDARY, -6.58, -4.6);

        graph.addNode(bN1); graph.addNode(bN2); graph.addNode(bN3);
        graph.addNode(bS1); graph.addNode(bS2); graph.addNode(bS3);
        graph.addNode(bE1); graph.addNode(bE2); graph.addNode(bE3);
        graph.addNode(bW1); graph.addNode(bW2); graph.addNode(bW3);
        graph.addNode(bN4); graph.addNode(bS4); graph.addNode(bS5);
        graph.addNode(bE4); graph.addNode(bE5); graph.addNode(bW4);

        // ==================== Intersection Nodes (82) - Real coordinates ====================
        // Wilson Blvd corridor
        Node n1  = new Node("1",  "Wilson Blvd & N Glebe Rd",      NodeType.INTERSECTION, -3.08, 1.05);
        Node n2  = new Node("2",  "Wilson Blvd & N Quincy St",     NodeType.INTERSECTION, -1.12, 1.51);
        Node n3  = new Node("3",  "Clarendon Blvd & N Highland St",NodeType.INTERSECTION, 0.47, 2.1);
        Node n4  = new Node("4",  "Wilson Blvd & N Courthouse Rd", NodeType.INTERSECTION, 1.7, 2.78);
        Node n5  = new Node("5",  "Ft Myer Dr & Wilson Blvd",      NodeType.INTERSECTION, 3.61, 4.2);
        // Lee Highway / Langston Blvd
        Node n6  = new Node("6",  "Lee Hwy & N Glebe Rd",          NodeType.INTERSECTION, -3.08, 1.93);
        Node n7  = new Node("7",  "Langston Blvd & N Quincy St",   NodeType.INTERSECTION, -1.12, 2.33);
        Node n8  = new Node("8",  "Washington Blvd & Kirkwood Rd", NodeType.INTERSECTION, -0.21, 2.78);
        Node n9  = new Node("9",  "Lee Hwy & N Lynn St",           NodeType.INTERSECTION, 3.88, 4.77);
        // Fairfax Dr corridor
        Node n10 = new Node("10", "Fairfax Dr & N Glebe Rd",       NodeType.INTERSECTION, -3.08, 0.51);
        Node n11 = new Node("11", "Fairfax Dr & N Quincy St",      NodeType.INTERSECTION, -1.12, 0.94);
        Node n12 = new Node("12", "Fairfax Dr & N Highland St",    NodeType.INTERSECTION, 0.47, 1.36);
        Node n13 = new Node("13", "Clarendon Blvd & Courthouse Rd",NodeType.INTERSECTION, 1.7, 1.93);
        Node n14 = new Node("14", "Ft Myer Dr & 19th St N",        NodeType.INTERSECTION, 3.61, 3.64);
        // Arlington Blvd / Rt 50
        Node n15 = new Node("15", "Arlington Blvd & George Mason Dr",NodeType.INTERSECTION, -5.45, -1.53);
        Node n16 = new Node("16", "Arlington Blvd & N Glebe Rd",   NodeType.INTERSECTION, -3.08, -1.19);
        Node n17 = new Node("17", "Arlington Blvd & N Quincy St",  NodeType.INTERSECTION, -1.12, -0.85);
        Node n18 = new Node("18", "Arlington Blvd & N Highland St", NodeType.INTERSECTION, 0.47, -0.62);
        Node n19 = new Node("19", "Arlington Blvd & Courthouse Rd",NodeType.INTERSECTION, 1.7, -0.4);
        Node n20 = new Node("20", "Arlington Blvd & N Lynn St",    NodeType.INTERSECTION, 3.61, -0.06);
        // Mid cross-streets
        Node n21 = new Node("21", "S Glebe Rd & 9th St S",         NodeType.INTERSECTION, -3.08, -3.46);
        Node n22 = new Node("22", "S Quincy St & 7th St S",        NodeType.INTERSECTION, -1.12, -3.24);
        Node n23 = new Node("23", "S Highland St & Columbia Pike",  NodeType.INTERSECTION, 0.47, -4.32);
        Node n24 = new Node("24", "S Courthouse Rd & Columbia Pike",NodeType.INTERSECTION, 1.7, -4.03);
        Node n25 = new Node("25", "S Lynn St & Pentagon Approach",  NodeType.INTERSECTION, 6.5, -2.0);
        // Columbia Pike corridor
        Node n26 = new Node("26", "Columbia Pike & George Mason Dr",NodeType.INTERSECTION, -5.45, -5.74);
        Node n27 = new Node("27", "Columbia Pike & S Glebe Rd",    NodeType.INTERSECTION, -3.08, -5.45);
        Node n28 = new Node("28", "Columbia Pike & S Walter Reed Dr",NodeType.INTERSECTION, -1.12, -5.17);
        Node n29 = new Node("29", "Columbia Pike & S Dinwiddie St", NodeType.INTERSECTION, 0.47, -5.03);
        Node n30 = new Node("30", "Columbia Pike & S Barton St",   NodeType.INTERSECTION, 1.7, -4.74);
        // Crystal City / Pentagon
        Node n31 = new Node("31", "Crystal Dr & 18th St S",        NodeType.INTERSECTION, 9.12, -6.08);
        Node n32 = new Node("32", "Crystal Dr & 23rd St S",        NodeType.INTERSECTION, 9.44, -7.72);
        Node n33 = new Node("33", "S Hayes St & 15th St S",        NodeType.INTERSECTION, 7.11, -4.6);
        // Cross-links
        Node n34 = new Node("34", "George Mason Dr & Pershing Dr", NodeType.INTERSECTION, -5.45, -0.06);
        Node n35 = new Node("35", "George Mason Dr & 3rd St S",    NodeType.INTERSECTION, -5.45, -3.75);
        Node n36 = new Node("36", "N Glebe Rd & 10th St N",        NodeType.INTERSECTION, -3.08, -0.34);
        // n37 removed (was Washington Blvd & N Glebe — degree too high, routed via n36)
        Node n38 = new Node("38", "N Lynn St & Key Blvd",          NodeType.INTERSECTION, 5.25, 3.92);
        // Rosslyn area
        Node n39 = new Node("39", "Rosslyn Circle & N Lynn St",    NodeType.INTERSECTION, 4.34, 4.66);
        Node n40 = new Node("40", "N Kent St & Lee Hwy",           NodeType.INTERSECTION, 4.91, 5.05);
        Node n41 = new Node("41", "N Nash St & Wilson Blvd",        NodeType.INTERSECTION, 4.57, 5.34);
        Node n42 = new Node("42", "N Meade St & Ft Myer Dr",       NodeType.INTERSECTION, 4.0, 4.34);
        Node n43 = new Node("43", "Lee Hwy & N Quinn St",          NodeType.INTERSECTION, 5.25, 5.2);
        Node n44 = new Node("44", "Rosslyn Metro & N Moore St",    NodeType.INTERSECTION, 4.22, 4.91);
        Node n45 = new Node("45", "Key Blvd & N Quinn St",         NodeType.INTERSECTION, 5.02, 4.06);
        Node n46 = new Node("46", "N Ft Myer Dr & Langston Blvd",  NodeType.INTERSECTION, 3.7, 4.63);
        // Pentagon / Crystal City (simplified: 5 nodes instead of 14)
        // n25 is the main entry (Army Navy Dr), n47 handles GW Pkwy exits,
        // n33 is Pentagon City hub, n31/n32 are Crystal City.
        // n48,n49,n50,n51,n52,n54,n80,n82 removed — too many nodes in small area.
        Node n47 = new Node("47", "GW Pkwy / Memorial Bridge Jct",  NodeType.INTERSECTION, 8.5, -0.5);
        // Ballston-Virginia Square
        Node n55 = new Node("55", "N Randolph St & Wilson Blvd",   NodeType.INTERSECTION, -4.54, 0.8);
        Node n56 = new Node("56", "Ballston Quarter & N Glebe Rd", NodeType.INTERSECTION, -4.08, 1.36);
        Node n57 = new Node("57", "N Stuart St & Lee Hwy",         NodeType.INTERSECTION, -4.54, 2.22);
        Node n58 = new Node("58", "Virginia Square Metro",         NodeType.INTERSECTION, -2.72, 1.02);
        Node n59 = new Node("59", "N Taylor St & Fairfax Dr",      NodeType.INTERSECTION, -4.08, 0.37);
        Node n60 = new Node("60", "N Stafford St & Washington Blvd",NodeType.INTERSECTION, -4.99, 2.5);
        Node n61 = new Node("61", "Ballston Common & N Randolph St",NodeType.INTERSECTION, -4.54, 1.65);
        // Shirlington
        Node n62 = new Node("62", "Shirlington Circle",            NodeType.INTERSECTION, 0.92, -7.44);
        Node n63 = new Node("63", "S Four Mile Run Dr & S Glebe Rd",NodeType.INTERSECTION, -0.67, -6.45);
        Node n64 = new Node("64", "Walter Reed Dr & S Columbia Pike",NodeType.INTERSECTION, 0.7, -6.16);
        Node n65 = new Node("65", "S Walter Reed Dr & S Glebe Rd", NodeType.INTERSECTION, 0.01, -6.87);
        Node n66 = new Node("66", "Campbell Ave & Randolph St",    NodeType.INTERSECTION, 0.92, -8.01);
        Node n67 = new Node("67", "Shirlington Village",           NodeType.INTERSECTION, 0.92, -8.29);
        // Infill corridors
        Node n68 = new Node("68", "10th St N & N Quincy St",       NodeType.INTERSECTION, -1.12, 0.09);
        Node n69 = new Node("69", "10th St N & N Highland St",     NodeType.INTERSECTION, 0.47, 0.37);
        Node n70 = new Node("70", "10th St N & N Courthouse Rd",   NodeType.INTERSECTION, 1.7, 0.65);
        Node n71 = new Node("71", "10th St N & N Lynn St",         NodeType.INTERSECTION, 3.61, 1.08);
        Node n72 = new Node("72", "Henderson Rd & S Glebe Rd",    NodeType.INTERSECTION, -3.08, -2.33);
        Node n73 = new Node("73", "Henderson Rd & S Quincy St",   NodeType.INTERSECTION, -1.12, -2.19);
        Node n74 = new Node("74", "Henderson Rd & S Highland St", NodeType.INTERSECTION, 0.47, -2.04);
        // n75 removed (was Henderson Rd & S Courthouse — isolated after degree fixes)
        Node n76 = new Node("76", "Pershing Dr & N Glebe Rd",     NodeType.INTERSECTION, -3.08, 0.17);
        // n77, n78, n79 removed (Washington Blvd/Clarendon infill — caused degree-5 nodes)
        // n80, n82 removed (Pentagon Row / 12th St S — part of Pentagon simplification)

        // Removed: n37,n48,n49,n50,n51,n52,n53,n54,n75,n77,n78,n79,n80,n81,n82
        Node[] intersections = {n1,n2,n3,n4,n5,n6,n7,n8,n9,n10,n11,n12,n13,n14,
            n15,n16,n17,n18,n19,n20,n21,n22,n23,n24,n25,n26,n27,n28,n29,n30,
            n31,n32,n33,n34,n35,n36,n38,
            n39,n40,n41,n42,n43,n44,n45,n46,
            n47,
            n55,n56,n57,n58,n59,n60,n61,
            n62,n63,n64,n65,n66,n67,
            n68,n69,n70,n71,n72,n73,n74,n76};
        for (Node n : intersections) graph.addNode(n);

        // ==================== Edges (73 bidirectional pairs = 146 directed) ====================

        // Wilson Blvd (E-W, y=8)
        graph.addBidirectionalEdge("E1","E2", n1, n2, 0.75);
        graph.addBidirectionalEdge("E3","E4", n2, n3, 0.75);
        graph.addBidirectionalEdge("E5","E6", n3, n4, 0.75);
        graph.addBidirectionalEdge("E7","E8", n4, n5, 0.75);

        // Lee Hwy (E-W, y=9)
        graph.addBidirectionalEdge("E9","E10", n6, n7, 0.75);
        graph.addBidirectionalEdge("E11","E12", n7, n8, 0.75);
        graph.addBidirectionalEdge("E13","E14", n8, n9, 1.5);

        // Fairfax Dr (E-W, y=7)
        graph.addBidirectionalEdge("E15","E16", n10, n11, 0.75);
        graph.addBidirectionalEdge("E17","E18", n11, n12, 0.75);
        graph.addBidirectionalEdge("E19","E20", n12, n13, 0.75);
        graph.addBidirectionalEdge("E21","E22", n13, n14, 0.75);

        // Arlington Blvd / Rt 50 (E-W, y=5)
        graph.addBidirectionalEdge("E23","E24", n15, n16, 0.75);
        graph.addBidirectionalEdge("E25","E26", n16, n17, 0.75);
        graph.addBidirectionalEdge("E27","E28", n17, n18, 0.75);
        graph.addBidirectionalEdge("E29","E30", n18, n19, 0.75);
        graph.addBidirectionalEdge("E31","E32", n19, n20, 0.75);

        // Mid cross-streets (E-W): n21→n22→n23→n24; n24→n25 long cross-map edge removed
        graph.addBidirectionalEdge("E33","E34", n21, n22, 0.75);
        graph.addBidirectionalEdge("E35","E36", n22, n23, 0.75);
        graph.addBidirectionalEdge("E37","E38", n23, n24, 0.75);

        // Columbia Pike (E-W, y=2.5)
        graph.addBidirectionalEdge("E41","E42", n26, n27, 0.75);
        graph.addBidirectionalEdge("E43","E44", n27, n28, 0.75);
        graph.addBidirectionalEdge("E45","E46", n28, n29, 0.75);
        graph.addBidirectionalEdge("E47","E48", n29, n30, 0.75);

        // N Glebe Rd (N-S): n6→n1→n10→n76→n36→n16→n21→n27 (n10-n36 direct removed; n76 mediates)
        graph.addBidirectionalEdge("E49","E50", n6, n1, 0.5);
        graph.addBidirectionalEdge("E51","E52", n1, n10, 0.5);
        graph.addBidirectionalEdge("E55","E56", n36, n16, 0.5);
        graph.addBidirectionalEdge("E57","E58", n16, n21, 0.75);
        graph.addBidirectionalEdge("E59","E60", n21, n27, 0.5);

        // N Quincy St (N-S, x=4.5)
        graph.addBidirectionalEdge("E61","E62", n7, n2, 0.5);
        graph.addBidirectionalEdge("E63","E64", n2, n11, 0.5);
        graph.addBidirectionalEdge("E65","E66", n11, n17, 1.0);
        graph.addBidirectionalEdge("E67","E68", n17, n22, 0.75);
        graph.addBidirectionalEdge("E69","E70", n22, n28, 0.5);

        // N Highland St (N-S, x=6.0)
        graph.addBidirectionalEdge("E71","E72", n8, n3, 0.5);
        graph.addBidirectionalEdge("E73","E74", n3, n12, 0.5);
        graph.addBidirectionalEdge("E75","E76", n12, n18, 1.0);
        graph.addBidirectionalEdge("E77","E78", n18, n23, 0.75);
        graph.addBidirectionalEdge("E79","E80", n23, n29, 0.5);

        // N Courthouse Rd (N-S, x=7.5)
        graph.addBidirectionalEdge("E81","E82", n4, n13, 0.5);
        graph.addBidirectionalEdge("E83","E84", n13, n19, 1.0);
        graph.addBidirectionalEdge("E85","E86", n19, n24, 0.75);
        graph.addBidirectionalEdge("E87","E88", n24, n30, 0.5);

        // N Lynn St / Pentagon corridor (N-S, x=9.0)
        graph.addBidirectionalEdge("E89","E90", n9, n5, 0.5);
        graph.addBidirectionalEdge("E91","E92", n5, n14, 0.5);
        graph.addBidirectionalEdge("E93","E94", n14, n20, 1.0);
        graph.addBidirectionalEdge("E95","E96", n20, n25, 0.75);
        graph.addBidirectionalEdge("E97","E98", n25, n33, 0.75);

        // George Mason Dr (N-S, x=1.5)
        graph.addBidirectionalEdge("E99","E100", n34, n15, 1.0);
        graph.addBidirectionalEdge("E101","E102", n15, n35, 0.75);
        graph.addBidirectionalEdge("E103","E104", n35, n26, 0.5);

        // Crystal City
        graph.addBidirectionalEdge("E107","E108", n31, n32, 0.75);

        // ==================== EXPANSION: New Edges ====================

        // Rosslyn area (n5-n39 removed — n5 deg fix; n39 reaches n5 via n41→bE1 path)
        graph.addBidirectionalEdge("E149","E150", n39, n41, 0.5);
        graph.addBidirectionalEdge("E151","E152", n41, bE1, 0.5);
        graph.addBidirectionalEdge("E153","E154", n9, n46, 0.5);
        graph.addBidirectionalEdge("E155","E156", n46, n40, 0.5);
        graph.addBidirectionalEdge("E157","E158", n40, n43, 0.5);
        graph.addBidirectionalEdge("E159","E160", n43, bE4, 1.0);
        graph.addBidirectionalEdge("E161","E162", n39, n42, 0.5);
        graph.addBidirectionalEdge("E163","E164", n42, n38, 0.5);
        graph.addBidirectionalEdge("E165","E166", n42, n44, 0.5);
        graph.addBidirectionalEdge("E167","E168", n44, n45, 0.5);
        graph.addBidirectionalEdge("E169","E170", n41, n40, 0.6);
        graph.addBidirectionalEdge("EX1","EX2",  n43, n45, 0.6); // N Quinn St connects Lee Hwy & Key Blvd nodes

        // Pentagon / Crystal City (simplified 5-node cluster)
        graph.addBidirectionalEdge("E171","E172", n25, n47, 1.0);  // Army Navy Dr → GW Pkwy junction
        graph.addBidirectionalEdge("EX3","EX4",   n47, bE2, 1.2);  // Memorial Bridge
        graph.addBidirectionalEdge("EX5","EX6",   n47, bE5, 0.8);  // Arlington Ridge Rd
        graph.addBidirectionalEdge("EX7","EX8",   n33, n31, 1.5);  // Pentagon City → Crystal City

        // Ballston-Virginia Square (n56-n6 removed — n6 deg fix; n58-n10 kept for Virginia Square connectivity)
        graph.addBidirectionalEdge("E199","E200", n55, n1, 0.75);
        graph.addBidirectionalEdge("E201","E202", n55, n56, 0.5);
        graph.addBidirectionalEdge("E205","E206", n57, n6, 0.75);
        graph.addBidirectionalEdge("E207","E208", n57, n60, 0.5);
        graph.addBidirectionalEdge("E209","E210", n60, bN4, 0.5);
        graph.addBidirectionalEdge("E211","E212", n60, n61, 0.5);
        graph.addBidirectionalEdge("E213","E214", n61, n56, 0.5);
        graph.addBidirectionalEdge("E215","E216", n58, n10, 0.5);
        graph.addBidirectionalEdge("E217","E218", n55, n58, 0.5);
        graph.addBidirectionalEdge("E219","E220", n59, n34, 0.5);
        graph.addBidirectionalEdge("E221","E222", n58, n59, 0.5);

        // Shirlington area (n27-n62 removed — n27 deg fix; reach Shirlington via n28→n64→n65→n62)
        graph.addBidirectionalEdge("E225","E226", n62, n67, 0.5);
        graph.addBidirectionalEdge("E227","E228", n67, bS5, 0.5);
        graph.addBidirectionalEdge("E229","E230", n62, n63, 0.5);
        graph.addBidirectionalEdge("E231","E232", n63, n66, 0.5);
        graph.addBidirectionalEdge("E233","E234", n28, n64, 0.5);
        graph.addBidirectionalEdge("E235","E236", n64, n65, 0.5);
        graph.addBidirectionalEdge("E237","E238", n65, n62, 0.5);
        graph.addBidirectionalEdge("E239","E240", n66, bS4, 0.75);

        // Infill: 10th St N corridor (y=6)
        graph.addBidirectionalEdge("E241","E242", n36, n68, 0.75);
        graph.addBidirectionalEdge("E243","E244", n68, n69, 0.75);
        graph.addBidirectionalEdge("E245","E246", n69, n70, 0.75);
        graph.addBidirectionalEdge("E247","E248", n70, n71, 0.75);
        graph.addBidirectionalEdge("E249","E250", n71, n38, 0.75);

        // Henderson Rd corridor (n74-n75 removed — n75 deleted)
        graph.addBidirectionalEdge("E251","E252", n72, n73, 0.75);
        graph.addBidirectionalEdge("E253","E254", n73, n74, 0.75);

        // N-S infill connections (n17-n68, n18-n69, n19-n70 removed — degree fixes)
        // (n16-n72 removed — n16 deg fix; n73-n22, n74-n23, n74-n75/n75-n24 removed — degree+n75 removal)
        graph.addBidirectionalEdge("E259","E260", n68, n73, 0.75);
        graph.addBidirectionalEdge("E263","E264", n69, n74, 0.75);
        graph.addBidirectionalEdge("E269","E270", n20, n71, 0.5);
        graph.addBidirectionalEdge("E273","E274", n72, n21, 0.5);

        // Pershing Dr / N Glebe: n10→n76→n36 (n76-n77/n77-n78/n78-n12 removed — n77,n78 deleted)
        graph.addBidirectionalEdge("E285","E286", n10, n76, 0.5);
        graph.addBidirectionalEdge("E287","E288", n76, n36, 0.5);
        // n79 (Clarendon Metro) removed — n2-n79, n79-n3, n79-n77 all removed
        // n81 (Columbia Pike infill) removed — n28-n81, n81-n29 removed

        // Additional boundary connections (n51-n20 removed — n20 deg fix)
        graph.addBidirectionalEdge("E303","E304", bW4, n35, 0.75);

        // Original boundary connections
        graph.addBidirectionalEdge("E123","E124", bN1, n6, 0.5);
        graph.addBidirectionalEdge("E125","E126", bN2, n8, 0.5);
        graph.addBidirectionalEdge("E127","E128", bN3, n9, 0.5);
        graph.addBidirectionalEdge("E129","E130", bS1, n27, 0.5);
        graph.addBidirectionalEdge("E131","E132", bS2, n33, 0.5);
        graph.addBidirectionalEdge("E133","E134", bS3, n32, 0.5);
        graph.addBidirectionalEdge("E135","E136", bE1, n5, 1.5);
        // bE2 connects to n47 (Pentagon GW Pkwy junction) — added above as EX3-EX4
        graph.addBidirectionalEdge("E139","E140", bE3, n31, 0.9);
        graph.addBidirectionalEdge("E141","E142", bW1, n15, 0.75);
        graph.addBidirectionalEdge("E143","E144", bW2, n34, 0.75);
        graph.addBidirectionalEdge("E145","E146", bW3, n26, 0.75);

        log.info("Loaded expanded Arlington road network - Nodes: {}, Edges: {}",
            graph.getNodeCount(), graph.getEdgeCount());

        return graph;
    }

    /**
     * Get basic graph information
     */
    private Map<String, Object> getGraphInfo(Graph graph) {
        Map<String, Object> info = new HashMap<>();
        info.put("nodeCount", graph.getNodeCount());
        info.put("edgeCount", graph.getEdgeCount());
        info.put("intersectionCount", graph.getIntersectionNodes().size());
        info.put("boundaryCount", graph.getBoundaryNodes().size());
        return info;
    }
}
