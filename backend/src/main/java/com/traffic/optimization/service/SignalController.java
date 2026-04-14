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
 * 2. TRAFFIC_ADAPTIVE: Webster 公式 + 实时交通量自适应
 * 3. LEARNING_BASED: Q-Learning 强化学习优化
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

    // ========== Q-Learning 参数 ==========
    /**
     * Q-Table: (节点ID + 状态) -> 各动作的 Q 值
     * 状态 = 等待车辆数的区间 (0-5, 5-15, 15-30, 30+)
     * 动作 = 绿灯时长 (15, 20, 25, 30, 40, 50, 60秒)
     */
    private final Map<String, double[]> qTable = new ConcurrentHashMap<>();
    private static final int[] ACTIONS = {15, 20, 25, 30, 40, 50, 60}; // 可选的绿灯时长
    private static final double LEARNING_RATE = 0.1;   // α
    private static final double DISCOUNT_FACTOR = 0.9; // γ
    private static final double EPSILON = 0.15;        // ε-greedy 探索率
    private final Random random = new Random();

    // ========== 多目标奖励权重 ==========
    private static final double W_QUEUE      = 0.5;   // 排队改善
    private static final double W_THROUGHPUT = 0.2;   // 吞吐量增量
    private static final double W_SPEED      = 0.2;   // 速度流畅度
    private static final double W_STABILITY  = 0.1;   // 绿灯抖动惩罚

    // 上次每个路口的等待车辆数（用于计算 reward）
    private final Map<String, Integer> lastWaitingCounts = new ConcurrentHashMap<>();

    // ========== TD 正确性追踪（修复 Q(s_t, a_t) 的更新目标）==========
    /** 每节点上一轮的 state key（用于 bootstrap 到正确的 Q-cell） */
    private final Map<String, String>  prevStateKey       = new ConcurrentHashMap<>();
    /** 每节点上一轮的 action index */
    private final Map<String, Integer> prevActionIdx      = new ConcurrentHashMap<>();
    /** 每节点上一轮的吞吐量（用于 R_throughput） */
    private final Map<String, Integer> lastThroughputNode = new ConcurrentHashMap<>();
    /** 每节点上一轮的绿灯时长（用于 R_stability） */
    private final Map<String, Integer> lastGreenAtNode    = new ConcurrentHashMap<>();

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
                // 固定时长模式，不做优化
                break;
            case TRAFFIC_ADAPTIVE:
                optimizeByWebster();
                break;
            case LEARNING_BASED:
                optimizeByQLearning();
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
    private void optimizeByWebster() {
        for (Node node : graph.getIntersectionNodes()) {
            TrafficLight light = node.getTrafficLight();
            if (light == null) continue;

            // 计算各方向的等待/通行车辆
            int waitingVehicles = flowManager.getWaitingFlowsAtNode(node);
            int totalActiveFlows = countActiveFlowsAtNode(node);

            // 估算交通流量比 y（简化：用等待车辆数/饱和流量估算）
            // 饱和流量假设: 每相位每秒通过 0.5 辆车（1800辆/小时）
            double saturationFlow = 0.5; // 辆/秒
            double demandRate = (waitingVehicles + totalActiveFlows) / 60.0; // 估算每秒到达率

            // 两个相位的流量比
            double y1 = Math.min(demandRate / saturationFlow, 0.9); // 限制最大 0.9
            double y2 = Math.min(demandRate / saturationFlow * 0.7, 0.9); // 假设交叉方向 70%

            double totalY = y1 + y2;

            // 防止除零和不合理值
            if (totalY >= 1.0) {
                totalY = 0.95; // 过饱和时使用最大周期
            }

            // 总损失时间 L = 启动损失(2s/相位 × 2相位) + 全红间隔
            double L = 4.0 + light.getAllRedDuration() * 2;

            // Webster 最优周期: C₀ = (1.5L + 5) / (1 - ΣYᵢ)
            double optimalCycle = (1.5 * L + 5) / (1 - totalY);

            // 限制周期在合理范围 [40, 180] 秒
            optimalCycle = Math.max(40, Math.min(180, optimalCycle));

            // 计算有效绿灯时间
            double effectiveGreen = optimalCycle - L;

            // 按流量比分配绿灯时间给各相位
            int greenTime;
            if (totalY > 0) {
                greenTime = (int) Math.round(effectiveGreen * y1 / totalY);
            } else {
                greenTime = (int) Math.round(effectiveGreen / 2);
            }

            // 应用优化后的绿灯时长
            light.adjustGreenDuration(greenTime);
            log.debug("Webster优化 节点{}: 周期={}s, 绿灯={}s, Y={:.2f}",
                node.getId(), (int) optimalCycle, greenTime, totalY);
        }
    }

    /**
     * 统计某节点附近的活跃交通流数
     */
    private int countActiveFlowsAtNode(Node node) {
        int count = 0;
        for (var flow : flowManager.getActiveFlowsList()) {
            if (flow.getCurrentNode() != null && flow.getCurrentNode().equals(node)) {
                count += flow.getNumberOfCars();
            }
        }
        return count;
    }

    // ==================== Q-Learning 优化 ====================

    /**
     * Q-Learning 强化学习优化信号时序
     *
     * 状态 (State): 路口等待车辆数的离散区间
     * 动作 (Action): 选择绿灯时长
     * 奖励 (Reward): 多目标加权（排队改善 + 吞吐量增量 + 速度流畅度 + 抖动惩罚）
     *
     * Bellman: Q(s_t, a_t) ← Q(s_t, a_t) + α[r_{t+1} + γ·max Q(s_{t+1}, ·) - Q(s_t, a_t)]
     *
     * 注意：更新的是 **上一轮** 的 (state, action)，不是当前轮 —
     * 因为 reward 是 (s_t, a_t) → s_{t+1} 这个转移产生的结果。
     */
    private void optimizeByQLearning() {
        for (Node node : graph.getIntersectionNodes()) {
            TrafficLight light = node.getTrafficLight();
            if (light == null) continue;

            String nodeId = node.getId();

            // 1. 观察 s_{t+1}（当前状态）
            int currentWaiting = flowManager.getWaitingFlowsAtNode(node)
                                 + countActiveFlowsAtNode(node);
            String state = getState(currentWaiting);
            String stateKey = nodeId + ":" + state;

            // 2. 计算多目标 reward（对应刚刚发生的 s_t → s_{t+1} 转移）
            double reward = calculateMultiObjectiveReward(node, light, currentWaiting);

            // 3. TD 更新 Q(s_t, a_t)（而非 s_{t+1}, a_{t+1}！）
            String prevKey = prevStateKey.get(nodeId);
            Integer prevAct = prevActionIdx.get(nodeId);
            if (prevKey != null && prevAct != null) {
                double[] prevQ = qTable.computeIfAbsent(prevKey, k -> new double[ACTIONS.length]);
                double maxNextQ = getMaxQ(stateKey);
                double currentQ = prevQ[prevAct];
                prevQ[prevAct] = currentQ
                    + LEARNING_RATE * (reward + DISCOUNT_FACTOR * maxNextQ - currentQ);
            }

            // 4. ε-greedy 选择 a_{t+1}
            int actionIndex = selectAction(stateKey);
            int greenDuration = ACTIONS[actionIndex];

            // 5. 执行动作
            light.adjustGreenDuration(greenDuration);

            // 6. 登记下一轮要用的上下文
            prevStateKey.put(nodeId, stateKey);
            prevActionIdx.put(nodeId, actionIndex);
            lastWaitingCounts.put(nodeId, currentWaiting);
            lastThroughputNode.put(nodeId, currentThroughputAtNode(node));
            lastGreenAtNode.put(nodeId, greenDuration);

            log.debug("Q-Learning {} state={} action={}s reward={}",
                nodeId, state, greenDuration, String.format("%.3f", reward));
        }
    }

    /**
     * 多目标奖励函数
     * R = w1·R_queue + w2·R_throughput + w3·R_speed + w4·R_stability
     * 每个子项都先裁剪到 [-1, 1]，保证权重可解释、避免单项主导。
     */
    private double calculateMultiObjectiveReward(Node node, TrafficLight light, int currentWaiting) {
        String nodeId = node.getId();

        // R1: 排队改善（tanh 软归一化，10 辆车 ≈ 饱和）
        int lastWaiting = lastWaitingCounts.getOrDefault(nodeId, currentWaiting);
        double rQueue = Math.tanh((lastWaiting - currentWaiting) / 10.0);

        // R2: 路口吞吐量增量
        int currTp = currentThroughputAtNode(node);
        int lastTp = lastThroughputNode.getOrDefault(nodeId, currTp);
        double rThroughput = Math.min(1.0, Math.max(0, currTp - lastTp) / 5.0);

        // R3: 路口周边边的平均速度比，映射到 [-1, 1]
        double rSpeed = 2.0 * averageSpeedRatioAroundNode(node) - 1.0;

        // R4: 绿灯时长抖动惩罚（0 秒 = 无罚，>= 45 秒 = 最大 -1）
        int newGreen = light.getGreenDuration();
        int prevGreen = lastGreenAtNode.getOrDefault(nodeId, newGreen);
        double rStability = -Math.min(1.0, Math.abs(newGreen - prevGreen) / 45.0);

        double reward = W_QUEUE * rQueue
                      + W_THROUGHPUT * rThroughput
                      + W_SPEED * rSpeed
                      + W_STABILITY * rStability;
        return Math.max(-1.0, Math.min(1.0, reward));
    }

    /** 路口周边边上的当前车辆总数（吞吐量代理指标） */
    private int currentThroughputAtNode(Node node) {
        int cars = 0;
        if (node.getIncomingEdges() != null) {
            for (Edge e : node.getIncomingEdges()) cars += e.getCurrentVehicleCount();
        }
        if (node.getOutgoingEdges() != null) {
            for (Edge e : node.getOutgoingEdges()) cars += e.getCurrentVehicleCount();
        }
        return cars;
    }

    /** 路口周边边的平均 actualSpeed / speedLimit 比值（流畅率） */
    private double averageSpeedRatioAroundNode(Node node) {
        double sum = 0;
        int n = 0;
        if (node.getIncomingEdges() != null) {
            for (Edge e : node.getIncomingEdges()) {
                if (e.getSpeedLimit() > 0) {
                    sum += e.getActualSpeed() / e.getSpeedLimit();
                    n++;
                }
            }
        }
        if (node.getOutgoingEdges() != null) {
            for (Edge e : node.getOutgoingEdges()) {
                if (e.getSpeedLimit() > 0) {
                    sum += e.getActualSpeed() / e.getSpeedLimit();
                    n++;
                }
            }
        }
        return n == 0 ? 1.0 : sum / n;
    }

    /**
     * 将等待车辆数映射为离散状态
     */
    private String getState(int waitingCount) {
        if (waitingCount <= 5) return "LOW";
        if (waitingCount <= 15) return "MEDIUM";
        if (waitingCount <= 30) return "HIGH";
        return "VERY_HIGH";
    }

    /**
     * ε-greedy 动作选择
     */
    private int selectAction(String stateKey) {
        if (random.nextDouble() < EPSILON) {
            // 探索：随机选择
            return random.nextInt(ACTIONS.length);
        }

        // 利用：选择 Q 值最大的动作
        double[] qValues = qTable.computeIfAbsent(stateKey, k -> new double[ACTIONS.length]);
        int bestAction = 0;
        double bestQ = qValues[0];
        for (int i = 1; i < qValues.length; i++) {
            if (qValues[i] > bestQ) {
                bestQ = qValues[i];
                bestAction = i;
            }
        }
        return bestAction;
    }

    /**
     * 获取某状态下的最大 Q 值
     */
    private double getMaxQ(String stateKey) {
        double[] qValues = qTable.getOrDefault(stateKey, new double[ACTIONS.length]);
        double maxQ = qValues[0];
        for (int i = 1; i < qValues.length; i++) {
            maxQ = Math.max(maxQ, qValues[i]);
        }
        return maxQ;
    }

    /**
     * 计算奖励函数
     * 等待车辆减少 → 正奖励，增加 → 负奖励
     */
    private double calculateReward(int lastWaiting, int currentWaiting) {
        int improvement = lastWaiting - currentWaiting;
        if (improvement > 0) {
            return Math.min(improvement * 0.5, 5.0); // 正奖励，上限5
        } else if (improvement < 0) {
            return Math.max(improvement * 0.8, -5.0); // 负奖励，下限-5
        }
        return 0.1; // 保持不变给微小正奖励
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
        TRAFFIC_ADAPTIVE,   // Webster 公式自适应模式
        LEARNING_BASED      // Q-Learning 强化学习模式
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
