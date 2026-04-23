package com.traffic.optimization.service;

import com.traffic.optimization.model.Edge;
import com.traffic.optimization.model.Graph;
import com.traffic.optimization.model.Node;
import com.traffic.optimization.model.NodeType;
import com.traffic.optimization.model.TrafficLight;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Signal controller - manages all intersection traffic lights.
 * Supports three optimization modes:
 * 1. FIXED_TIME: fixed timing (no optimization)
 * 2. TRAFFIC_ADAPTIVE: Webster formula + wait-time-weighted asymmetric adaptive control
 * 3. GREEN_WAVE: phase-offset coordination along the main corridor at a design speed
 *
 * @author Chengkun Liao, Mingjie Shen
 */
@Service
@Getter
public class SignalController {

    private static final Logger log = LoggerFactory.getLogger(SignalController.class);

    private Graph graph;
    private FlowManager flowManager;
    private OptimizationMode mode;
    private List<OptimizationRecord> optimizationHistory;

    // ========== Green Wave coordination parameters ==========
    /**
     * Green Wave uses the same symmetric timing as FIXED (20/20, cycle=50s),
     * relying solely on offset coordination for its advantage.
     * Off-corridor flows face the same maximum wait as in FIXED mode, while
     * corridor flows gain zero-stop benefit for free.
     */
    private static final int GW_EW_GREEN = 20;
    private static final int GW_NS_GREEN = 20;
    /** Design speed for Green Wave (km/h) - used to calculate the offset at each intersection */
    private static final double GW_DESIGN_SPEED_KMH = 40.0;
    /** Simulation slowdown factor (matches the *2 factor in Edge.getIdealTravelTime) */
    private static final double GW_SLOWDOWN = 2.0;
    /** Corridor detection: Y-coordinate bin resolution (intersections on the same EW corridor have similar y values) */
    private static final double GW_Y_BIN = 0.5;
    /** Corridor detection: minimum number of nodes; corridors smaller than this are not coordinated */
    private static final int GW_MIN_CORRIDOR_SIZE = 3;

    /** Green Wave initialization flag - phase synchronization only happens on the first entry into GREEN_WAVE mode */
    private boolean greenWaveInitialized = false;

    public SignalController() {
        this.mode = OptimizationMode.FIXED_TIME;
        this.optimizationHistory = new ArrayList<>();
    }

    public void setGraph(Graph graph) {
        this.graph = graph;
        // Assign a random initial phase to each signal to avoid all intersections being
        // perfectly synchronized at sim_time=0 (which would make FIXED mode look like
        // centralized control visually). This does not affect FIXED algorithm behavior
        // (each intersection still runs its own independent fixed cycle); it only staggers
        // the starting points. Green Wave mode will override these via synchronize()
        // inside initializeGreenWave(), so this has no effect there.
        Random r = new Random(42); // fixed seed -> reproducible results
        for (Node node : graph.getIntersectionNodes()) {
            TrafficLight light = node.getTrafficLight();
            if (light != null) {
                light.synchronize(r.nextInt(light.getCycleLength()));
            }
        }
    }

    public void setFlowManager(FlowManager flowManager) {
        this.flowManager = flowManager;
    }

    public void setOptimizationMode(OptimizationMode mode) {
        if (this.mode != mode) {
            // Clear the initialization flag when leaving GREEN_WAVE so the next entry re-synchronizes
            if (this.mode == OptimizationMode.GREEN_WAVE) greenWaveInitialized = false;
        }
        this.mode = mode;
        log.info("Signal optimization mode switched to: {}", mode);
    }

    /**
     * Update all traffic lights (called every second)
     */
    public void updateSignals() {
        if (graph == null) {
            return;
        }
        for (Node node : graph.getIntersectionNodes()) {
            if (node.getTrafficLight() != null) {
                node.getTrafficLight().update();
            }
        }
    }

    /**
     * Optimize signal timing (called periodically)
     */
    public void optimizeSignals() {
        if (graph == null || flowManager == null) {
            return;
        }

        switch (mode) {
            case FIXED_TIME:
                // Fixed timing mode - no optimization performed
                break;
            case TRAFFIC_ADAPTIVE:
                optimizeByWebster();
                break;
            case GREEN_WAVE:
                if (!greenWaveInitialized) {
                    initializeGreenWave();
                    greenWaveInitialized = true;
                }
                // Subsequent calls do not re-synchronize (re-synchronizing would disrupt the green wave phase)
                break;
        }
    }

    // ==================== Webster formula optimization ====================

    /**
     * Signal optimization based on the Webster formula.
     *
     * Webster's optimal cycle formula: C0 = (1.5L + 5) / (1 - sum(yi))
     * - C0: optimal cycle length (seconds)
     * - L:  total lost time (startup loss + all-red interval)
     * - yi: flow ratio for each phase (actual flow / saturation flow)
     *
     * Reference: F.V. Webster, "Traffic Signal Settings", 1958
     */
    private static final double WAIT_PENALTY_TAU = 30.0;     // wait-time normalization scale (seconds)
    private static final double SATURATION_FLOW = 0.5;       // saturation flow rate (vehicles/s ~= 1800/h)
    private static final double DEMAND_WINDOW   = 30.0;      // demand observation window (seconds)

    private static final int    MIN_CYCLE      = 40;   // minimum cycle length (seconds)
    private static final int    MAX_CYCLE      = 80;   // maximum cycle length (seconds) - shorter aids network coordination
    private static final int    MIN_DIR_GREEN  = 15;   // minimum green per direction - hard anti-starvation constraint

    private void optimizeByWebster() {
        for (Node node : graph.getIntersectionNodes()) {
            TrafficLight light = node.getTrafficLight();
            if (light == null) continue;

            // Aggregate incoming-approach demand by direction (with wait-time weighting)
            DirectionalDemand dEW = new DirectionalDemand();
            DirectionalDemand dNS = new DirectionalDemand();
            for (Edge in : node.getIncomingEdges()) {
                TrafficLight.SignalDirection dir = edgeDirection(in);
                DirectionalDemand bucket = (dir == TrafficLight.SignalDirection.EAST_WEST) ? dEW : dNS;
                accumulateEdgeDemand(in, bucket);
            }

            // yi uses the weighted demand: longer wait -> larger y -> longer cycle ->
            // algorithm naturally grants more green to the congested direction
            double yEW = Math.min(dEW.weighted / (DEMAND_WINDOW * SATURATION_FLOW), 0.9);
            double yNS = Math.min(dNS.weighted / (DEMAND_WINDOW * SATURATION_FLOW), 0.9);
            double totalY = yEW + yNS;
            if (totalY >= 0.95) totalY = 0.95;

            double L = 4.0 + light.getAllRedDuration() * 2;
            double optimalCycle = (totalY > 0) ? (1.5 * L + 5) / (1 - totalY) : MIN_CYCLE;
            optimalCycle = Math.max(MIN_CYCLE, Math.min(MAX_CYCLE, optimalCycle));
            double effectiveGreen = optimalCycle - L;

            // Allocate green time proportionally to weighted demand,
            // but enforce MIN_DIR_GREEN on each direction as a hard anti-starvation constraint
            double wEW = dEW.weighted, wNS = dNS.weighted;
            double splitEW, splitNS;
            if (wEW + wNS > 0) {
                splitEW = wEW / (wEW + wNS);
                splitNS = wNS / (wEW + wNS);
            } else {
                splitEW = 0.5;
                splitNS = 0.5;
            }

            int greenEW = (int) Math.round(effectiveGreen * splitEW);
            int greenNS = (int) Math.round(effectiveGreen * splitNS);
            // Ensure both directions get a reasonable green window
            if (greenEW < MIN_DIR_GREEN) {
                greenNS -= (MIN_DIR_GREEN - greenEW);
                greenEW = MIN_DIR_GREEN;
            }
            if (greenNS < MIN_DIR_GREEN) {
                greenEW -= (MIN_DIR_GREEN - greenNS);
                greenNS = MIN_DIR_GREEN;
            }
            greenEW = Math.max(MIN_DIR_GREEN, greenEW);
            greenNS = Math.max(MIN_DIR_GREEN, greenNS);
            light.adjustGreenDurations(greenEW, greenNS);

            if (log.isDebugEnabled()) {
                log.debug(String.format("Webster node %s: C=%ds green EW=%ds/NS=%ds cars=%d/%d weighted=%.1f/%.1f",
                    node.getId(), (int) optimalCycle, greenEW, greenNS,
                    dEW.rawCars, dNS.rawCars, wEW, wNS));
            }
        }
    }

    /** Determine the direction of an edge (from the dominant displacement component) */
    private TrafficLight.SignalDirection edgeDirection(Edge edge) {
        double dx = edge.getToNode().getX() - edge.getFromNode().getX();
        double dy = edge.getToNode().getY() - edge.getFromNode().getY();
        return Math.abs(dx) > Math.abs(dy)
            ? TrafficLight.SignalDirection.EAST_WEST
            : TrafficLight.SignalDirection.NORTH_SOUTH;
    }

    /** Accumulate raw vehicle count and wait-weighted vehicle count for one incoming approach */
    private void accumulateEdgeDemand(Edge edge, DirectionalDemand bucket) {
        Queue<com.traffic.optimization.model.TrafficFlow> queue = edge.getVehicleQueue();
        if (queue == null) return;
        for (var flow : queue) {
            int cars = flow.getNumberOfCars();
            bucket.rawCars += cars;
            // Penalty factor (1 + wait/tau)^2: wait=0 -> 1x; wait=tau -> 4x; wait=2tau -> 9x
            double w = 1.0 + flow.getCurrentWaitTime() / WAIT_PENALTY_TAU;
            bucket.weighted += cars * w * w;
        }
    }

    private static class DirectionalDemand {
        int rawCars = 0;
        double weighted = 0.0;
    }

    // ==================== Green Wave Coordination ====================

    /**
     * Green Wave Coordination
     *
     * Concept: assign phase offsets to intersections along the main corridor based on
     * travel time, so that vehicles travelling at the design speed arrive at each
     * intersection exactly when its green phase begins.
     *
     * 1. Identify EW corridors: sequences of intersections near the same Y coordinate
     *    (grouped by GW_Y_BIN, sorted by X).
     * 2. For each corridor, use the first intersection as the reference and compute
     *    each intersection's travel time from cumulative distance / design speed.
     * 3. Call synchronize(phase) to set the intersection's phase at sim_time=0 to
     *    (cycle - travelTime) % cycle, so that at sim_time=travelTime the intersection
     *    is exactly at the start of its EW green phase.
     *
     * Limitation: only the EW main direction is coordinated. The NS direction uses
     * the same cycle but is not coordinated (extending to a full two-way green wave
     * would be more complex).
     */
    private void initializeGreenWave() {
        // Set uniform green durations for all intersections - a consistent cycle is required for coordination
        for (Node node : graph.getIntersectionNodes()) {
            TrafficLight light = node.getTrafficLight();
            if (light != null) light.adjustGreenDurations(GW_EW_GREEN, GW_NS_GREEN);
        }

        int cycle = 0;
        {
            List<Node> inters = graph.getIntersectionNodes();
            if (!inters.isEmpty() && inters.get(0).getTrafficLight() != null) {
                cycle = inters.get(0).getTrafficLight().getCycleLength();
            }
        }
        if (cycle == 0) return;

        // Identify corridors from graph connectivity: union-find over all EW edges between intersections,
        // then sort each connected component by X and accumulate actual travel time along EW edge chains.
        // This avoids the bug of forcing disconnected intersections into the same corridor via coarse Y binning.
        Map<String, Node> nodeById = new HashMap<>();
        Map<String, String> parent = new HashMap<>();
        for (Node n : graph.getIntersectionNodes()) {
            nodeById.put(n.getId(), n);
            parent.put(n.getId(), n.getId());
        }
        // union-find helpers
        java.util.function.Function<String, String> find = new java.util.function.Function<>() {
            @Override public String apply(String x) {
                String p = parent.get(x);
                while (p != null && !p.equals(x)) {
                    String gp = parent.get(p);
                    parent.put(x, gp);
                    x = p;
                    p = parent.get(x);
                }
                return x;
            }
        };

        // Collect all EW edges whose endpoints are both intersections and union their endpoints
        List<Edge> ewEdges = new ArrayList<>();
        for (Edge e : graph.getEdges()) {
            Node a = e.getFromNode();
            Node b = e.getToNode();
            if (a == null || b == null) continue;
            if (a.getType() != NodeType.INTERSECTION || b.getType() != NodeType.INTERSECTION) continue;
            double dx = Math.abs(b.getX() - a.getX());
            double dy = Math.abs(b.getY() - a.getY());
            if (dx > dy) {
                ewEdges.add(e);
                String ra = find.apply(a.getId());
                String rb = find.apply(b.getId());
                if (!ra.equals(rb)) parent.put(ra, rb);
            }
        }

        // Group nodes by connected component
        Map<String, List<Node>> corridors = new HashMap<>();
        for (String id : nodeById.keySet()) {
            String root = find.apply(id);
            // Only include nodes that actually participate in EW edges (filter out pure NS islands)
            corridors.computeIfAbsent(root, k -> new ArrayList<>()).add(nodeById.get(id));
        }

        // Adjacency list: EW edges only (stored as Edge objects to use idealTravelTime directly)
        Map<String, List<Edge>> ewOutgoing = new HashMap<>();
        for (Edge e : ewEdges) {
            ewOutgoing.computeIfAbsent(e.getFromNode().getId(), k -> new ArrayList<>()).add(e);
        }

        int corridorsCoordinated = 0;
        int nodesCoordinated = 0;

        for (List<Node> corridor : corridors.values()) {
            // Only keep nodes that actually appear on EW edges - skip isolated nodes (no EW edges)
            List<Node> ewNodes = new ArrayList<>();
            for (Node n : corridor) {
                if (ewOutgoing.containsKey(n.getId())
                    || ewEdges.stream().anyMatch(e -> e.getToNode().getId().equals(n.getId()))) {
                    ewNodes.add(n);
                }
            }
            if (ewNodes.size() < GW_MIN_CORRIDOR_SIZE) continue;

            // Sort by X (westernmost first), then BFS along EW edge chains to accumulate actual travel seconds.
            // Uses Edge.getIdealTravelTime directly to stay perfectly aligned with the simulation.
            ewNodes.sort(Comparator.comparingDouble(Node::getX));
            Node root = ewNodes.get(0);
            Map<String, Double> travelSecMap = new HashMap<>();
            travelSecMap.put(root.getId(), 0.0);
            ArrayDeque<Node> queue = new ArrayDeque<>();
            queue.add(root);
            while (!queue.isEmpty()) {
                Node cur = queue.poll();
                List<Edge> outs = ewOutgoing.getOrDefault(cur.getId(), Collections.emptyList());
                for (Edge e : outs) {
                    Node nxt = e.getToNode();
                    double edgeSec = e.getIdealTravelTime() * 60.0; // minutes -> seconds
                    double newTravel = travelSecMap.get(cur.getId()) + edgeSec;
                    if (!travelSecMap.containsKey(nxt.getId()) || newTravel < travelSecMap.get(nxt.getId())) {
                        travelSecMap.put(nxt.getId(), newTravel);
                        queue.add(nxt);
                    }
                }
            }

            // Bi-directional Green Wave (Little 1966):
            // W->E optimal offset = ti mod C  (cumulative travel time)
            // E->W optimal offset = (T - ti) mod C  (T = total corridor travel time)
            //
            // Both cannot be satisfied simultaneously (unless the inter-intersection spacing
            // happens to be exactly C/2). Compromise strategy:
            // Compare the W->E offset and E->W offset phase difference.
            // If their green windows overlap, choose the midpoint to satisfy both directions;
            // otherwise, prefer the W->E (main commute) direction.
            double totalSec = 0;
            for (Node n : ewNodes) {
                Double s = travelSecMap.get(n.getId());
                if (s != null && s > totalSec) totalSec = s;
            }
            int T = (int) Math.round(totalSec);
            int greenWindow = GW_EW_GREEN; // 20s
            int halfWindow = greenWindow / 2;

            for (Node n : ewNodes) {
                Double t = travelSecMap.get(n.getId());
                if (t == null) continue;
                int ti = (int) Math.round(t);
                // Offsets needed for each direction (mod C)
                int offsetEW = ((ti % cycle) + cycle) % cycle;
                int offsetWE = (((T - ti) % cycle) + cycle) % cycle;
                // Circular phase difference in [0, C/2]
                int diff = Math.abs(offsetEW - offsetWE);
                if (diff > cycle / 2) diff = cycle - diff;

                int chosenOffset;
                if (diff <= greenWindow) {
                    // Green windows overlap - choose the midpoint so both directions fall within green
                    // Simple circular average
                    chosenOffset = ((offsetEW + offsetWE) / 2) % cycle;
                } else {
                    // No overlap - prefer W->E (main commute direction)
                    chosenOffset = offsetEW;
                }

                int phaseAtT0 = (cycle - chosenOffset) % cycle;
                TrafficLight light = n.getTrafficLight();
                if (light != null) {
                    light.synchronize(phaseAtT0);
                    nodesCoordinated++;
                }
            }
            corridorsCoordinated++;
        }

        log.info("Green Wave bi-directional coordination complete: cycle={}s EW={}s NS={}s, corridors={}, nodes={} (based on Edge.idealTravelTime)",
                cycle, GW_EW_GREEN, GW_NS_GREEN, corridorsCoordinated, nodesCoordinated);
    }

    // ==================== Utility methods ====================

    public void setSignalTiming(String nodeId, int greenDuration) {
        Node node = graph.getNode(nodeId);
        if (node != null && node.getType() == NodeType.INTERSECTION) {
            TrafficLight light = node.getTrafficLight();
            if (light != null) {
                light.adjustGreenDuration(greenDuration);
            }
        }
    }

    public void synchronizeSignals() {
        List<Node> intersections = graph.getIntersectionNodes();
        if (intersections.isEmpty()) return;

        for (Node node : intersections) {
            TrafficLight light = node.getTrafficLight();
            if (light != null) {
                light.setCurrentDirection(TrafficLight.SignalDirection.EAST_WEST);
                light.setCurrentState(TrafficLight.SignalState.GREEN);
                light.setRemainingTime(light.getGreenDuration());
            }
        }
    }

    public List<SignalStatus> getAllSignalStatuses() {
        List<SignalStatus> statuses = new ArrayList<>();

        for (Node node : graph.getIntersectionNodes()) {
            TrafficLight light = node.getTrafficLight();
            if (light != null) {
                SignalStatus status = new SignalStatus(
                    node.getId(),
                    node.getName(),
                    light.getCurrentDirection(),
                    light.getCurrentState(),
                    light.getRemainingTime(),
                    light.getGreenDuration()
                );
                statuses.add(status);
            }
        }

        return statuses;
    }

    public void recordOptimization(double efficiency) {
        OptimizationRecord record = new OptimizationRecord(
            System.currentTimeMillis(), mode, efficiency
        );
        optimizationHistory.add(record);
        if (optimizationHistory.size() > 100) {
            optimizationHistory.remove(0);
        }
    }

    // ==================== Inner classes ====================

    public enum OptimizationMode {
        FIXED_TIME,         // Fixed timing mode
        TRAFFIC_ADAPTIVE,   // Webster formula + wait-time-weighted adaptive mode
        GREEN_WAVE          // Green wave coordination (phase offsets along the main corridor at design speed)
    }

    @Getter
    public static class SignalStatus {
        private String nodeId;
        private String nodeName;
        private TrafficLight.SignalDirection direction;
        private TrafficLight.SignalState state;
        private int remainingTime;
        private int greenDuration;

        public SignalStatus(String nodeId, String nodeName,
                          TrafficLight.SignalDirection direction,
                          TrafficLight.SignalState state,
                          int remainingTime, int greenDuration) {
            this.nodeId = nodeId;
            this.nodeName = nodeName;
            this.direction = direction;
            this.state = state;
            this.remainingTime = remainingTime;
            this.greenDuration = greenDuration;
        }
    }

    @Getter
    public static class OptimizationRecord {
        private long timestamp;
        private OptimizationMode mode;
        private double efficiency;

        public OptimizationRecord(long timestamp, OptimizationMode mode, double efficiency) {
            this.timestamp = timestamp;
            this.mode = mode;
            this.efficiency = efficiency;
        }
    }
}
