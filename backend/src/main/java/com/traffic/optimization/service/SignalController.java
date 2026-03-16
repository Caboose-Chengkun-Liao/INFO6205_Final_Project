package com.traffic.optimization.service;

import com.traffic.optimization.model.Graph;
import com.traffic.optimization.model.Node;
import com.traffic.optimization.model.NodeType;
import com.traffic.optimization.model.TrafficLight;
import lombok.Getter;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 信号灯控制器 - 负责管理所有路口的信号灯
 *
 * @author Chengkun Liao, Mingjie Shen
 */
@Service
@Getter
public class SignalController {

    /**
     * 道路网络图
     */
    private Graph graph;

    /**
     * 流量管理器
     */
    private FlowManager flowManager;

    /**
     * 优化模式
     */
    private OptimizationMode mode;

    /**
     * 优化历史记录
     */
    private List<OptimizationRecord> optimizationHistory;

    /**
     * 构造函数
     */
    public SignalController() {
        this.mode = OptimizationMode.FIXED_TIME;
        this.optimizationHistory = new ArrayList<>();
    }

    /**
     * 设置道路网络图
     */
    public void setGraph(Graph graph) {
        this.graph = graph;
    }

    /**
     * 设置流量管理器
     */
    public void setFlowManager(FlowManager flowManager) {
        this.flowManager = flowManager;
    }

    /**
     * 设置优化模式
     */
    public void setOptimizationMode(OptimizationMode mode) {
        this.mode = mode;
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
     * 优化信号灯时序
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
                optimizeByTrafficVolume();
                break;

            case LEARNING_BASED:
                optimizeByLearning();
                break;
        }
    }

    /**
     * 基于交通量的自适应优化
     */
    private void optimizeByTrafficVolume() {
        for (Node node : graph.getIntersectionNodes()) {
            TrafficLight light = node.getTrafficLight();
            if (light == null) {
                continue;
            }

            // 计算该路口的等待车辆数
            int waitingVehicles = flowManager.getWaitingFlowsAtNode(node);

            // 根据等待车辆数调整绿灯时长
            int newGreenDuration = calculateOptimalGreenTime(waitingVehicles);

            // 应用新的绿灯时长
            light.adjustGreenDuration(newGreenDuration);
        }
    }

    /**
     * 计算最优绿灯时间
     */
    private int calculateOptimalGreenTime(int waitingVehicles) {
        // 基础时长
        int baseTime = 20;

        // 根据等待车辆数增加时长（每10辆车增加5秒）
        int additionalTime = (waitingVehicles / 10) * 5;

        // 总时长限制在15-60秒之间
        return Math.max(15, Math.min(60, baseTime + additionalTime));
    }

    /**
     * 基于学习的优化（简化版）
     */
    private void optimizeByLearning() {
        // TODO: 实现基于历史数据的学习优化
        // 这里可以使用强化学习或其他机器学习算法

        // 当前简化实现：使用历史平均效率来调整
        optimizeByTrafficVolume();
    }

    /**
     * 手动设置单个路口的信号时长
     */
    public void setSignalTiming(String nodeId, int greenDuration) {
        Node node = graph.getNode(nodeId);
        if (node != null && node.getType() == NodeType.INTERSECTION) {
            TrafficLight light = node.getTrafficLight();
            if (light != null) {
                light.adjustGreenDuration(greenDuration);
            }
        }
    }

    /**
     * 同步所有信号灯（使其协调工作）
     */
    public void synchronizeSignals() {
        List<Node> intersections = graph.getIntersectionNodes();
        if (intersections.isEmpty()) {
            return;
        }

        // 获取主干道路口
        // 简化实现：将所有东西向信号灯同步
        for (Node node : intersections) {
            TrafficLight light = node.getTrafficLight();
            if (light != null) {
                // 重置到相同的初始状态
                light.setCurrentDirection(TrafficLight.SignalDirection.EAST_WEST);
                light.setCurrentState(TrafficLight.SignalState.GREEN);
                light.setRemainingTime(light.getGreenDuration());
            }
        }
    }

    /**
     * 获取所有信号灯状态
     */
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

    /**
     * 记录优化结果
     */
    public void recordOptimization(double efficiency) {
        OptimizationRecord record = new OptimizationRecord(
            System.currentTimeMillis(),
            mode,
            efficiency
        );
        optimizationHistory.add(record);

        // 只保留最近100条记录
        if (optimizationHistory.size() > 100) {
            optimizationHistory.remove(0);
        }
    }

    /**
     * 优化模式枚举
     */
    public enum OptimizationMode {
        FIXED_TIME,         // 固定时长模式
        TRAFFIC_ADAPTIVE,   // 交通自适应模式
        LEARNING_BASED      // 基于学习的模式
    }

    /**
     * 信号状态类
     */
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

    /**
     * 优化记录类
     */
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
