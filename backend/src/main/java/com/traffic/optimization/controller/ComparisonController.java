package com.traffic.optimization.controller;

import com.traffic.optimization.model.*;
import com.traffic.optimization.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.*;

/**
 * 并行模式对比控制器 — 同时运行 FIXED / ADAPTIVE / INTELLIGENT 三种信号模式
 * 每种模式使用独立的仿真引擎实例和图拷贝
 *
 * @author Chengkun Liao, Mingjie Shen
 */
@RestController
@RequestMapping("/api/compare")
@CrossOrigin(origins = "*")
public class ComparisonController {

    private static final Logger log = LoggerFactory.getLogger(ComparisonController.class);

    private static final String[] MODE_NAMES = {"FIXED_TIME", "TRAFFIC_ADAPTIVE", "LEARNING_BASED"};
    private static final String[] MODE_LABELS = {"Fixed Timing", "Adaptive", "AI-Optimized"};

    @Autowired
    private SimulationController simulationController;

    /** 3 independent simulation instances */
    private SimulationEngine[] engines = new SimulationEngine[3];
    private FlowManager[] flowManagers = new FlowManager[3];
    private SignalController[] signalControllers = new SignalController[3];
    private EfficiencyCalculator[] efficiencyCalculators = new EfficiencyCalculator[3];
    private Graph[] graphs = new Graph[3];

    private ScheduledExecutorService scheduler;
    private volatile boolean running = false;

    /** Historical efficiency snapshots for trend chart */
    private final List<Map<String, Object>> trendHistory = Collections.synchronizedList(new ArrayList<>());
    private long lastTrendRecordTime = 0;
    private static final long TREND_RECORD_INTERVAL = 15; // record every 15 simulation seconds

    /**
     * Start parallel comparison — creates 3 independent simulations with predefined traffic flows
     */
    @PostMapping("/start")
    public Map<String, Object> startComparison(@RequestBody(required = false) Map<String, Object> config) {
        if (running) {
            stopComparison();
        }

        // Get the base graph from the main simulation controller
        Graph baseGraph = getBaseGraph();
        if (baseGraph == null) {
            return Map.of("error", "Please initialize the main simulation first");
        }

        // Create 3 independent simulation instances
        for (int i = 0; i < 3; i++) {
            graphs[i] = baseGraph.deepCopy();
            flowManagers[i] = new FlowManager();
            signalControllers[i] = new SignalController();
            efficiencyCalculators[i] = new EfficiencyCalculator();

            engines[i] = new SimulationEngine();
            engines[i].setDependencies(flowManagers[i], signalControllers[i], efficiencyCalculators[i]);
            engines[i].initialize(graphs[i]);

            // Set signal mode
            signalControllers[i].setOptimizationMode(
                    SignalController.OptimizationMode.valueOf(MODE_NAMES[i]));

            // Disable continuous random flow generation for controlled comparison
            engines[i].setContinuousFlowEnabled(false);

            // Give each mode distinct initial signal timing to differentiate from the start
            if (i == 0) {
                // FIXED: short cycle (20s green) — represents a naive fixed timing
                for (Node node : graphs[i].getIntersectionNodes()) {
                    if (node.getTrafficLight() != null) {
                        node.getTrafficLight().adjustGreenDuration(20);
                    }
                }
            } else if (i == 2) {
                // AI-OPTIMIZED: longer initial cycle (45s green) — Q-Learning will tune from here
                for (Node node : graphs[i].getIntersectionNodes()) {
                    if (node.getTrafficLight() != null) {
                        node.getTrafficLight().adjustGreenDuration(45);
                    }
                }
            }
            // ADAPTIVE (i==1): keeps default 30s, Webster will optimize immediately

            engines[i].start();
        }

        // Inject identical traffic flows into all 3 simulations
        List<Map<String, Object>> defaultFlows = getDefaultFlows();
        for (Map<String, Object> flowConfig : defaultFlows) {
            String entry = (String) flowConfig.get("entryPoint");
            String dest = (String) flowConfig.get("destination");
            int cars = (int) flowConfig.get("numberOfCars");

            for (int i = 0; i < 3; i++) {
                createFlowInEngine(i, entry, dest, cars);
            }
        }

        // Trigger immediate signal optimization so modes diverge from the start
        for (int i = 0; i < 3; i++) {
            signalControllers[i].optimizeSignals();
        }

        // Reset trend history
        trendHistory.clear();
        lastTrendRecordTime = 0;

        // Start parallel stepping — 5 steps per tick (5x speed) for faster divergence
        running = true;
        scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(() -> {
            if (!running) return;
            for (int step = 0; step < 5; step++) {
                for (int i = 0; i < 3; i++) {
                    try {
                        engines[i].step();
                        if (engines[i].getCurrentTime() % 30 == 0) {
                            signalControllers[i].optimizeSignals();
                        }
                    } catch (Exception e) {
                        log.error("Comparison engine {} step error: {}", i, e.getMessage());
                    }
                }
            }

            // Record efficiency snapshot for trend chart
            long simTime = engines[0].getCurrentTime();
            if (simTime - lastTrendRecordTime >= TREND_RECORD_INTERVAL) {
                Map<String, Object> record = new HashMap<>();
                record.put("timestamp", simTime);
                for (int i = 0; i < 3; i++) {
                    record.put(MODE_NAMES[i], computeLiveEfficiency(i));
                }
                trendHistory.add(record);
                // Cap history to last 200 points
                if (trendHistory.size() > 200) {
                    trendHistory.remove(0);
                }
                lastTrendRecordTime = simTime;
            }
        }, 0, 1000, TimeUnit.MILLISECONDS);

        log.info("Started parallel comparison with 3 modes");

        Map<String, Object> result = new HashMap<>();
        result.put("status", "running");
        result.put("modes", MODE_LABELS);
        result.put("flowsPerMode", defaultFlows.size());
        return result;
    }

    /**
     * Stop all parallel simulations
     */
    @PostMapping("/stop")
    public Map<String, Object> stopComparison() {
        running = false;
        if (scheduler != null) {
            scheduler.shutdown();
            scheduler = null;
        }
        for (int i = 0; i < 3; i++) {
            if (engines[i] != null) {
                engines[i].stop();
            }
        }
        log.info("Stopped parallel comparison");
        return Map.of("status", "stopped");
    }

    /**
     * Get vehicle positions for a specific simulation instance
     */
    @GetMapping("/{index}/vehicles")
    public List<Map<String, Object>> getVehicles(@PathVariable int index) {
        if (index < 0 || index > 2 || engines[index] == null || flowManagers[index] == null) {
            return Collections.emptyList();
        }

        List<Map<String, Object>> vehicles = new ArrayList<>();
        for (TrafficFlow flow : flowManagers[index].getActiveFlowsList()) {
            if (flow.getCurrentEdge() == null) continue;
            Map<String, Object> v = new HashMap<>();
            v.put("flowId", flow.getFlowId());
            v.put("numberOfCars", flow.getNumberOfCars());
            v.put("state", flow.getState().name());
            v.put("currentEdge", flow.getCurrentEdge().getId());
            v.put("from", flow.getCurrentEdge().getFromNode().getId());
            v.put("to", flow.getCurrentEdge().getToNode().getId());
            double idealTime = flow.getCurrentEdge().getIdealTravelTime();
            v.put("progress", Math.min(1.0, flow.getTimeOnCurrentEdge() / ((idealTime > 0 ? idealTime : 1) * 60)));
            vehicles.add(v);
        }
        return vehicles;
    }

    /**
     * Get graph data (with edge loads) for a specific simulation instance
     */
    @GetMapping("/{index}/graph")
    public Map<String, Object> getGraph(@PathVariable int index) {
        if (index < 0 || index > 2 || graphs[index] == null) {
            return Map.of("error", "Invalid index or not started");
        }

        Graph g = graphs[index];
        List<Map<String, Object>> nodeList = new ArrayList<>();
        for (Node n : g.getAllNodes()) {
            Map<String, Object> nm = new HashMap<>();
            nm.put("id", n.getId());
            nm.put("name", n.getName());
            nm.put("type", n.getType().name());
            nm.put("x", n.getX());
            nm.put("y", n.getY());
            nodeList.add(nm);
        }

        List<Map<String, Object>> edgeList = new ArrayList<>();
        for (Edge e : g.getEdges()) {
            Map<String, Object> em = new HashMap<>();
            em.put("id", e.getId());
            em.put("from", e.getFromNode().getId());
            em.put("to", e.getToNode().getId());
            em.put("distance", e.getDistance());
            em.put("currentLoad", e.getCurrentVehicleCount());
            edgeList.add(em);
        }

        return Map.of("nodes", nodeList, "edges", edgeList);
    }

    /**
     * Get comparison metrics for all 3 simulations
     */
    @GetMapping("/metrics")
    public List<Map<String, Object>> getMetrics() {
        List<Map<String, Object>> metrics = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            Map<String, Object> m = new HashMap<>();
            m.put("mode", MODE_LABELS[i]);
            m.put("modeKey", MODE_NAMES[i]);

            if (engines[i] != null && flowManagers[i] != null) {
                m.put("currentTime", engines[i].getCurrentTime());
                EfficiencyCalculator.PerformanceMetrics pm = engines[i].getCurrentMetrics();
                if (pm != null) {
                    m.put("efficiency", pm.getEfficiency());
                    m.put("avgSpeed", pm.getAvgSpeed());
                    m.put("throughput", pm.getThroughput());
                    m.put("activeFlows", pm.getActiveFlowCount());
                    m.put("completedFlows", pm.getCompletedFlowCount());
                    m.put("avgTravelTime", pm.getAvgTravelTime());
                    // Network-level metrics (new)
                    m.put("networkOccupancy", pm.getNetworkOccupancy());
                    m.put("congestedEdgeRatio", pm.getCongestedEdgeRatio());
                    m.put("avgQueueLength", pm.getAvgQueueLength());
                    m.put("stoppedVehicleRate", pm.getStoppedVehicleRate());
                    m.put("speedReductionRatio", pm.getSpeedReductionRatio());
                }

                // Real-time metrics from active flows (more useful before any flow completes)
                List<TrafficFlow> active = flowManagers[i].getActiveFlowsList();
                int totalCars = 0;
                int blockedFlows = 0;
                double totalProgress = 0;
                for (TrafficFlow f : active) {
                    totalCars += f.getNumberOfCars();
                    if (f.getState() == TrafficFlow.FlowState.BLOCKED) blockedFlows++;
                    if (f.getCurrentEdge() != null) {
                        double idealTime = f.getCurrentEdge().getIdealTravelTime();
                        double progress = idealTime > 0
                            ? Math.min(1.0, f.getTimeOnCurrentEdge() / (idealTime * 60))
                            : 0;
                        totalProgress += progress;
                    }
                }
                m.put("totalVehicles", totalCars);
                m.put("blockedFlows", blockedFlows);
                m.put("avgProgress", active.size() > 0 ? totalProgress / active.size() : 0);

                // Count congested edges (>50% capacity)
                int congestedEdges = 0;
                for (Edge e : graphs[i].getEdges()) {
                    double cap = e.getDistance() * e.getCapacityPerKm();
                    if (cap > 0 && e.getCurrentVehicleCount() / cap > 0.5) congestedEdges++;
                }
                m.put("congestedEdges", congestedEdges);
            } else {
                m.put("currentTime", 0);
                m.put("efficiency", 0);
                m.put("avgSpeed", 0);
                m.put("throughput", 0);
                m.put("activeFlows", 0);
                m.put("completedFlows", 0);
                m.put("totalVehicles", 0);
                m.put("blockedFlows", 0);
                m.put("avgProgress", 0);
                m.put("congestedEdges", 0);
                m.put("networkOccupancy", 0);
                m.put("congestedEdgeRatio", 0);
                m.put("avgQueueLength", 0);
                m.put("stoppedVehicleRate", 0);
                m.put("speedReductionRatio", 0);
            }
            metrics.add(m);
        }
        return metrics;
    }

    /**
     * Efficiency trend history across all 3 modes.
     * Returns [{ timestamp, FIXED_TIME, TRAFFIC_ADAPTIVE, LEARNING_BASED }, ...]
     */
    @GetMapping("/efficiency/trend")
    public List<Map<String, Object>> getEfficiencyTrend() {
        synchronized (trendHistory) {
            return new ArrayList<>(trendHistory);
        }
    }

    /**
     * Compute efficiency for engine[i] using both completed and active flows.
     * Formula: E = Σ(Ni × Li / ti) / Σ(Ni)
     */
    private double computeLiveEfficiency(int i) {
        if (flowManagers[i] == null) return 0.0;

        double numerator = 0.0;
        int denominator = 0;

        List<TrafficFlow> completed = flowManagers[i].getCompletedFlowsList();
        for (TrafficFlow f : completed) {
            int Ni = f.getNumberOfCars();
            double Li = f.getTotalDistance();
            double ti = f.getTravelTimeCounter() / 3600.0;
            if (ti > 0 && Li > 0) {
                numerator += (Ni * Li / ti);
                denominator += Ni;
            }
        }

        List<TrafficFlow> active = flowManagers[i].getActiveFlowsList();
        for (TrafficFlow f : active) {
            int Ni = f.getNumberOfCars();
            double Li = f.getTotalDistance();
            double ti = f.getTravelTimeCounter() / 3600.0;
            if (ti > 0 && Li > 0) {
                numerator += (Ni * Li / ti);
                denominator += Ni;
            }
        }

        return denominator > 0 ? numerator / denominator : 0.0;
    }

    /**
     * Get signal states for a specific simulation instance
     */
    @GetMapping("/{index}/signals")
    public List<?> getSignals(@PathVariable int index) {
        if (index < 0 || index > 2 || signalControllers[index] == null) {
            return Collections.emptyList();
        }
        return signalControllers[index].getAllSignalStatuses();
    }

    /**
     * Check comparison status
     */
    @GetMapping("/status")
    public Map<String, Object> getStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("running", running);
        if (running && engines[0] != null) {
            status.put("currentTime", engines[0].getCurrentTime());
            status.put("modes", MODE_LABELS);
        }
        return status;
    }

    // --- Private helpers ---

    private Graph getBaseGraph() {
        try {
            // Use reflection-free approach: the SimulationController creates the graph
            // We trigger initialization if needed and get the graph from the main engine
            java.lang.reflect.Method m = SimulationController.class.getDeclaredMethod("createDefaultGraph");
            m.setAccessible(true);
            return (Graph) m.invoke(simulationController);
        } catch (Exception e) {
            log.error("Failed to get base graph: {}", e.getMessage());
            return null;
        }
    }

    private void createFlowInEngine(int index, String entryId, String destId, int numberOfCars) {
        try {
            flowManagers[index].createFlow(entryId, destId, numberOfCars);
        } catch (Exception e) {
            log.warn("Failed to create flow in engine {}: {}", index, e.getMessage());
        }
    }

    private List<Map<String, Object>> getDefaultFlows() {
        List<Map<String, Object>> flows = new ArrayList<>();
        // Heavy commuter corridors (morning rush)
        flows.add(Map.of("entryPoint", "W2", "destination", "E1", "numberOfCars", 40));
        flows.add(Map.of("entryPoint", "W1", "destination", "E2", "numberOfCars", 35));
        flows.add(Map.of("entryPoint", "N1", "destination", "S2", "numberOfCars", 30));
        flows.add(Map.of("entryPoint", "W3", "destination", "E3", "numberOfCars", 25));
        flows.add(Map.of("entryPoint", "N2", "destination", "E1", "numberOfCars", 25));
        flows.add(Map.of("entryPoint", "S1", "destination", "N3", "numberOfCars", 20));
        flows.add(Map.of("entryPoint", "E1", "destination", "W1", "numberOfCars", 20));
        flows.add(Map.of("entryPoint", "N3", "destination", "S3", "numberOfCars", 15));
        // Cross-town traffic creating congestion at key intersections
        flows.add(Map.of("entryPoint", "W2", "destination", "S2", "numberOfCars", 20));
        flows.add(Map.of("entryPoint", "N1", "destination", "E3", "numberOfCars", 15));
        flows.add(Map.of("entryPoint", "W1", "destination", "S3", "numberOfCars", 15));
        flows.add(Map.of("entryPoint", "N4", "destination", "E1", "numberOfCars", 20));
        flows.add(Map.of("entryPoint", "W4", "destination", "E2", "numberOfCars", 10));
        flows.add(Map.of("entryPoint", "S5", "destination", "N2", "numberOfCars", 10));
        return flows;
    }
}
