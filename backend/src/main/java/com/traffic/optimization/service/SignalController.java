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
 * 信号灯控制器 - 负责管理所有路口的信号灯
 * 支持三种优化模式：
 * 1. FIXED_TIME: 固定时长（无优化）
 * 2. TRAFFIC_ADAPTIVE: Webster + 等待时间加权的非对称自适应
 * 3. GREEN_WAVE: 按设计车速对主走廊各路口设相位偏移的绿波协调
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

    // ========== 绿波协调参数 ==========
    /**
     * 绿波使用与 FIXED 相同的对称配时(20/20,cycle=50s),只靠 offset 协调取得优势。
     * 这样 off-corridor 的 flow 与 FIXED 等待上限一致,沿走廊 flow 白赚 0 stop 的收益。
     */
    private static final int GW_EW_GREEN = 20;
    private static final int GW_NS_GREEN = 20;
    /** 绿波设计车速(km/h) — 用于计算各路口间的 offset */
    private static final double GW_DESIGN_SPEED_KMH = 40.0;
    /** 仿真速度倍率(与 Edge.getIdealTravelTime 的 *2 系数一致) */
    private static final double GW_SLOWDOWN = 2.0;
    /** 走廊识别:Y 坐标聚类分辨率(同一条 EW 走廊的节点 y 约等) */
    private static final double GW_Y_BIN = 0.5;
    /** 走廊识别:最小节点数,少于此值的不算走廊(也就不协调) */
    private static final int GW_MIN_CORRIDOR_SIZE = 3;

    /** 绿波初始化标记 — 只在首次进入 GREEN_WAVE 模式时同步相位 */
    private boolean greenWaveInitialized = false;

    public SignalController() {
        this.mode = OptimizationMode.FIXED_TIME;
        this.optimizationHistory = new ArrayList<>();
    }

    public void setGraph(Graph graph) {
        this.graph = graph;
    }

    public void setFlowManager(FlowManager flowManager) {
        this.flowManager = flowManager;
    }

    public void setOptimizationMode(OptimizationMode mode) {
        if (this.mode != mode) {
            // 切换离开 GREEN_WAVE 时清标记,下次进入重新同步
            if (this.mode == OptimizationMode.GREEN_WAVE) greenWaveInitialized = false;
        }
        this.mode = mode;
        log.info("信号优化模式切换为: {}", mode);
    }

    /**
     * 更新所有信号灯（每秒调用）
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
     * 优化信号灯时序（定期调用）
     */
    public void optimizeSignals() {
        if (graph == null || flowManager == null) {
            return;
        }

        switch (mode) {
            case FIXED_TIME:
                // 固定时长模式,不做优化
                break;
            case TRAFFIC_ADAPTIVE:
                optimizeByWebster();
                break;
            case GREEN_WAVE:
                if (!greenWaveInitialized) {
                    initializeGreenWave();
                    greenWaveInitialized = true;
                }
                // 后续调用不再重新同步(重新同步会打断绿波相位)
                break;
        }
    }

    // ==================== Webster 公式优化 ====================

    /**
     * 基于 Webster 公式的信号优化
     *
     * Webster 最优周期公式: C₀ = (1.5L + 5) / (1 - Σyᵢ)
     * - C₀: 最优周期时长（秒）
     * - L: 总损失时间（启动损失 + 全红间隔）
     * - yᵢ: 各相位的交通流量比 (实际流量/饱和流量)
     *
     * 参考: F.V. Webster, "Traffic Signal Settings", 1958
     */
    private static final double WAIT_PENALTY_TAU = 30.0;     // 等待时间归一化尺度(秒)
    private static final double SATURATION_FLOW = 0.5;       // 饱和流率(辆/秒 ≈ 1800/h)
    private static final double DEMAND_WINDOW   = 30.0;      // 需求观测窗口(秒)

    private static final int    MIN_CYCLE      = 40;   // 最小周期(秒)
    private static final int    MAX_CYCLE      = 80;   // 最大周期(秒) - 更短利于网络协调
    private static final int    MIN_DIR_GREEN  = 15;   // 每个方向的绿灯下限 - 防饿死硬约束

    private void optimizeByWebster() {
        for (Node node : graph.getIntersectionNodes()) {
            TrafficLight light = node.getTrafficLight();
            if (light == null) continue;

            // 按方向统计进口道的需求(含等待时间加权)
            DirectionalDemand dEW = new DirectionalDemand();
            DirectionalDemand dNS = new DirectionalDemand();
            for (Edge in : node.getIncomingEdges()) {
                TrafficLight.SignalDirection dir = edgeDirection(in);
                DirectionalDemand bucket = (dir == TrafficLight.SignalDirection.EAST_WEST) ? dEW : dNS;
                accumulateEdgeDemand(in, bucket);
            }

            // y_i 使用加权需求:等待越久,y 越大,周期也越长 → 算法自然给拥堵方更多时间
            double yEW = Math.min(dEW.weighted / (DEMAND_WINDOW * SATURATION_FLOW), 0.9);
            double yNS = Math.min(dNS.weighted / (DEMAND_WINDOW * SATURATION_FLOW), 0.9);
            double totalY = yEW + yNS;
            if (totalY >= 0.95) totalY = 0.95;

            double L = 4.0 + light.getAllRedDuration() * 2;
            double optimalCycle = (totalY > 0) ? (1.5 * L + 5) / (1 - totalY) : MIN_CYCLE;
            optimalCycle = Math.max(MIN_CYCLE, Math.min(MAX_CYCLE, optimalCycle));
            double effectiveGreen = optimalCycle - L;

            // 按加权需求比例分配,但每方向不低于 MIN_DIR_GREEN (硬约束防饿死)
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
            // 保证两方向都有合理的通行窗口
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
                log.debug(String.format("Webster 节点%s: C=%ds green EW=%ds/NS=%ds cars=%d/%d weighted=%.1f/%.1f",
                    node.getId(), (int) optimalCycle, greenEW, greenNS,
                    dEW.rawCars, dNS.rawCars, wEW, wNS));
            }
        }
    }

    /** 判断一条边的走向(通过 from→to 位移主分量) */
    private TrafficLight.SignalDirection edgeDirection(Edge edge) {
        double dx = edge.getToNode().getX() - edge.getFromNode().getX();
        double dy = edge.getToNode().getY() - edge.getFromNode().getY();
        return Math.abs(dx) > Math.abs(dy)
            ? TrafficLight.SignalDirection.EAST_WEST
            : TrafficLight.SignalDirection.NORTH_SOUTH;
    }

    /** 累加一条进口道上的原始车数与等待加权车数 */
    private void accumulateEdgeDemand(Edge edge, DirectionalDemand bucket) {
        Queue<com.traffic.optimization.model.TrafficFlow> queue = edge.getVehicleQueue();
        if (queue == null) return;
        for (var flow : queue) {
            int cars = flow.getNumberOfCars();
            bucket.rawCars += cars;
            // 惩罚系数 (1 + wait/τ)²:wait=0 → 1×;wait=τ → 4×;wait=2τ → 9×
            double w = 1.0 + flow.getCurrentWaitTime() / WAIT_PENALTY_TAU;
            bucket.weighted += cars * w * w;
        }
    }

    private static class DirectionalDemand {
        int rawCars = 0;
        double weighted = 0.0;
    }

    // ==================== 绿波协调(Green Wave Coordination) ====================

    /**
     * 绿波协调(Green Wave Coordination)
     *
     * 思路:在主走廊上按"行车时间"给各路口加相位偏移,让设计车速下的车辆在每个路口
     * 到达时恰好赶上绿灯。
     *
     * 1. 识别 EW 走廊:同一 Y 坐标附近的路口序列(按 GW_Y_BIN 分组,按 X 排序)
     * 2. 对每条走廊,以第一个路口为参考,按累计距离 / 设计车速 计算每个路口的行车时间
     * 3. 用 synchronize(phase) 把该路口在 sim_time=0 时的相位设为 (cycle - travelTime) % cycle,
     *    这样 sim_time=travelTime 时该路口恰好处于 EW 绿灯起点
     *
     * 限制:只协调 EW 主方向。NS 方向用同一周期但不协调(可以扩展到双向绿波但实现更复杂)。
     */
    private void initializeGreenWave() {
        // 统一所有路口的绿灯时长 — 保证 cycle 一致才能协调
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

        // 基于图连通性识别走廊:把所有 EW 边两端的路口 union 到一起;
        // 再对每个连通分量按 X 排序,走 EW 边链累加"真实行车时间"作为 offset。
        // 避免了按 Y 坐标粗聚类导致把不连通的路口硬凑成走廊的 bug。
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

        // 收集所有 EW 边(起终点都是路口),union 两端
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

        // 按连通分量分组
        Map<String, List<Node>> corridors = new HashMap<>();
        for (String id : nodeById.keySet()) {
            String root = find.apply(id);
            // 只收录实际参与过 EW 边的节点(过滤纯 NS 孤岛)
            corridors.computeIfAbsent(root, k -> new ArrayList<>()).add(nodeById.get(id));
        }

        // 邻接表:仅 EW 边(存边对象以直接取 idealTravelTime,避免依赖估算速度)
        Map<String, List<Edge>> ewOutgoing = new HashMap<>();
        for (Edge e : ewEdges) {
            ewOutgoing.computeIfAbsent(e.getFromNode().getId(), k -> new ArrayList<>()).add(e);
        }

        int corridorsCoordinated = 0;
        int nodesCoordinated = 0;

        for (List<Node> corridor : corridors.values()) {
            // 只保留真正出现在 EW 边上的节点 —— 孤立节点(没有 EW 边)跳过
            List<Node> ewNodes = new ArrayList<>();
            for (Node n : corridor) {
                if (ewOutgoing.containsKey(n.getId())
                    || ewEdges.stream().anyMatch(e -> e.getToNode().getId().equals(n.getId()))) {
                    ewNodes.add(n);
                }
            }
            if (ewNodes.size() < GW_MIN_CORRIDOR_SIZE) continue;

            // 按 X 排序,从最西端节点开始 BFS 沿 EW 边链累加真实行车秒数
            // (直接用 Edge.getIdealTravelTime 避免依赖估算的设计车速 — 跟 sim 完全对齐)
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
                    double edgeSec = e.getIdealTravelTime() * 60.0; // 分钟 → 秒
                    double newTravel = travelSecMap.get(cur.getId()) + edgeSec;
                    if (!travelSecMap.containsKey(nxt.getId()) || newTravel < travelSecMap.get(nxt.getId())) {
                        travelSecMap.put(nxt.getId(), newTravel);
                        queue.add(nxt);
                    }
                }
            }

            // 双向绿波(Bi-directional Green Wave, Little 1966):
            // W→E 方向最优 offset = t_i mod C  (cumulative travel time)
            // E→W 方向最优 offset = (T - t_i) mod C  (T = 走廊总行车时间)
            //
            // 两者不可能同时 100% 满足(除非相邻路口间隔正好 C/2)。折中方案:
            // 根据该节点 W→E 方向 offset 与 E→W 方向 offset 的相位差,
            // 选择满足两个方向"绿灯窗口"并集最大的那个 offset。
            //
            // 等价启发式:若 W→E offset 与 E→W offset 相差 ≤ EW 绿时长,两者绿窗重叠 → 任选;
            // 否则选更接近 "绿窗中点" 的 offset。
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
                // 两个方向需要的 offset(mod C)
                int offsetEW = ((ti % cycle) + cycle) % cycle;
                int offsetWE = (((T - ti) % cycle) + cycle) % cycle;
                // 两 offset 在环上的相位差(取 [0, C/2])
                int diff = Math.abs(offsetEW - offsetWE);
                if (diff > cycle / 2) diff = cycle - diff;

                int chosenOffset;
                if (diff <= greenWindow) {
                    // 两方向绿窗重叠,选中点使两边都在绿窗中
                    // 环上平均 offset
                    int sum = offsetEW + offsetWE;
                    chosenOffset = (offsetEW - offsetWE + cycle) % cycle < cycle / 2
                        ? ((offsetEW + (offsetEW - offsetWE + cycle) % cycle / 2) % cycle)
                        : ((offsetEW + (offsetWE - offsetEW + cycle) % cycle / 2) % cycle);
                    // 简化:直接平均(环上处理)
                    chosenOffset = ((offsetEW + offsetWE) / 2) % cycle;
                } else {
                    // 不重叠,选 W→E(主通勤方向)
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

        log.info("Green Wave 双向协调完成: cycle={}s EW={}s NS={}s, 走廊={}, 节点={} (基于 Edge.idealTravelTime)",
                cycle, GW_EW_GREEN, GW_NS_GREEN, corridorsCoordinated, nodesCoordinated);
    }

    // ==================== 工具方法 ====================

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

    // ==================== 内部类 ====================

    public enum OptimizationMode {
        FIXED_TIME,         // 固定时长模式
        TRAFFIC_ADAPTIVE,   // Webster 公式 + 等待时间加权的自适应模式
        GREEN_WAVE          // 绿波协调(按设计车速给主走廊各路口设相位偏移)
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
