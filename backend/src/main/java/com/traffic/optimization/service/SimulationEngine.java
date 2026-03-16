package com.traffic.optimization.service;

import com.traffic.optimization.model.*;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 仿真引擎 - 负责整个交通系统的时间步进仿真
 *
 * @author Chengkun Liao, Mingjie Shen
 */
@Service
@Getter
public class SimulationEngine {

    @Autowired
    private FlowManager flowManager;

    @Autowired
    private SignalController signalController;

    @Autowired
    private EfficiencyCalculator efficiencyCalculator;

    /**
     * 道路网络图
     */
    private Graph graph;

    /**
     * 仿真状态
     */
    private SimulationState state;

    /**
     * 当前仿真时间（秒）
     */
    private long currentTime;

    /**
     * 时间步长（秒）
     */
    private double timeStep;

    /**
     * 仿真速度倍率
     */
    private double speedMultiplier;

    /**
     * 效率评估间隔（秒）
     */
    private long efficiencyEvaluationInterval;

    /**
     * 上次效率评估时间
     */
    private long lastEfficiencyEvaluationTime;

    /**
     * 构造函数
     */
    public SimulationEngine() {
        this.state = SimulationState.STOPPED;
        this.currentTime = 0;
        this.timeStep = 1.0; // 默认1秒
        this.speedMultiplier = 1.0;
        this.efficiencyEvaluationInterval = 3600; // 每小时评估一次
        this.lastEfficiencyEvaluationTime = 0;
    }

    /**
     * 初始化仿真
     */
    public void initialize(Graph graph) {
        this.graph = graph;
        this.flowManager.setGraph(graph);
        this.signalController.setGraph(graph);
        this.signalController.setFlowManager(flowManager);

        this.currentTime = 0;
        this.state = SimulationState.INITIALIZED;

        System.out.println("仿真引擎已初始化");
    }

    /**
     * 开始仿真
     */
    public void start() {
        if (state == SimulationState.INITIALIZED || state == SimulationState.PAUSED) {
            state = SimulationState.RUNNING;
            System.out.println("仿真已启动");
        }
    }

    /**
     * 暂停仿真
     */
    public void pause() {
        if (state == SimulationState.RUNNING) {
            state = SimulationState.PAUSED;
            System.out.println("仿真已暂停");
        }
    }

    /**
     * 停止仿真
     */
    public void stop() {
        state = SimulationState.STOPPED;
        currentTime = 0;
        System.out.println("仿真已停止");
    }

    /**
     * 重置仿真
     */
    public void reset() {
        stop();
        flowManager.clearAllFlows();
        efficiencyCalculator.clearHistory();
        currentTime = 0;
        lastEfficiencyEvaluationTime = 0;
        state = SimulationState.INITIALIZED;
        System.out.println("仿真已重置");
    }

    /**
     * 执行单个时间步（主仿真循环）
     */
    public void step() {
        if (state != SimulationState.RUNNING) {
            return;
        }

        // 1. 更新信号灯
        signalController.updateSignals();

        // 2. 更新交通流
        updateTrafficFlows();

        // 3. 更新流量管理器
        flowManager.updateFlows(timeStep);

        // 4. 增加仿真时间
        currentTime += (long) timeStep;

        // 5. 定期评估效率
        if (currentTime - lastEfficiencyEvaluationTime >= efficiencyEvaluationInterval) {
            evaluateEfficiency();
            lastEfficiencyEvaluationTime = currentTime;
        }

        // 6. 定期优化信号灯
        if (currentTime % 300 == 0) { // 每5分钟优化一次
            signalController.optimizeSignals();
        }
    }

    /**
     * 更新交通流的移动
     */
    private void updateTrafficFlows() {
        List<TrafficFlow> activeFlows = flowManager.getActiveFlowsList();

        for (TrafficFlow flow : activeFlows) {
            if (flow.getState() == TrafficFlow.FlowState.WAITING) {
                // 尝试进入网络
                tryEnterNetwork(flow);
            } else if (flow.getState() == TrafficFlow.FlowState.ACTIVE) {
                // 尝试移动到下一个节点
                tryMoveToNextNode(flow);
            }
        }
    }

    /**
     * 尝试让流进入网络
     */
    private void tryEnterNetwork(TrafficFlow flow) {
        Node currentNode = flow.getCurrentNode();
        Node nextNode = flow.getNextNode();

        if (nextNode == null) {
            return;
        }

        Edge edge = currentNode.getEdgeTo(nextNode);
        if (edge != null && !edge.isFull()) {
            // 检查信号灯（如果当前节点是路口）
            if (canPass(currentNode, nextNode)) {
                edge.addVehicle(flow);
                flow.setCurrentEdge(edge);
                flow.setState(TrafficFlow.FlowState.ACTIVE);
            }
        }
    }

    /**
     * 尝试移动到下一个节点
     */
    private void tryMoveToNextNode(TrafficFlow flow) {
        Edge currentEdge = flow.getCurrentEdge();
        if (currentEdge == null) {
            return;
        }

        // 检查是否已在当前边上停留足够时间
        double requiredTime = currentEdge.getIdealTravelTime() * 60; // 转换为秒
        if (flow.getTimeOnCurrentEdge() >= requiredTime) {
            // 尝试移动到下一个节点
            Node nextNode = flow.getNextNode();
            if (nextNode != null && canPass(flow.getCurrentNode(), nextNode)) {
                // 从当前边移除
                currentEdge.removeVehicle();

                // 移动到下一个节点
                flow.moveToNextNode();

                // 如果还有下一段路
                if (!flow.hasReachedDestination()) {
                    Node newNextNode = flow.getNextNode();
                    if (newNextNode != null) {
                        Edge newEdge = flow.getCurrentNode().getEdgeTo(newNextNode);
                        if (newEdge != null && !newEdge.isFull()) {
                            newEdge.addVehicle(flow);
                            flow.setCurrentEdge(newEdge);
                        } else {
                            flow.setState(TrafficFlow.FlowState.BLOCKED);
                        }
                    }
                }
            } else {
                flow.setState(TrafficFlow.FlowState.BLOCKED);
            }
        }
    }

    /**
     * 检查是否可以从一个节点通过到另一个节点
     */
    private boolean canPass(Node from, Node to) {
        // 如果起点不是路口，可以通过
        if (from.getType() != NodeType.INTERSECTION) {
            return true;
        }

        // 检查信号灯
        TrafficLight light = from.getTrafficLight();
        if (light == null) {
            return true;
        }

        // 简化实现：根据节点位置判断方向
        // TODO: 实现更精确的方向判断
        TrafficLight.SignalDirection direction = determineDirection(from, to);
        return light.canPass(direction);
    }

    /**
     * 判断行驶方向
     */
    private TrafficLight.SignalDirection determineDirection(Node from, Node to) {
        double dx = to.getX() - from.getX();
        double dy = to.getY() - from.getY();

        // 如果水平移动大于垂直移动，则为东西向
        if (Math.abs(dx) > Math.abs(dy)) {
            return TrafficLight.SignalDirection.EAST_WEST;
        } else {
            return TrafficLight.SignalDirection.NORTH_SOUTH;
        }
    }

    /**
     * 评估当前效率
     */
    private void evaluateEfficiency() {
        List<TrafficFlow> completedFlows = flowManager.getCompletedFlowsList();
        double efficiency = efficiencyCalculator.calculateEfficiency(completedFlows);

        efficiencyCalculator.recordEfficiency(efficiency, currentTime);
        signalController.recordOptimization(efficiency);

        System.out.printf("时间 %d 秒 - 效率: %.2f%n", currentTime, efficiency);
    }

    /**
     * 获取当前性能指标
     */
    public EfficiencyCalculator.PerformanceMetrics getCurrentMetrics() {
        return efficiencyCalculator.calculatePerformanceMetrics(
            flowManager.getActiveFlowsList(),
            flowManager.getCompletedFlowsList()
        );
    }

    /**
     * 设置时间步长
     */
    public void setTimeStep(double timeStep) {
        this.timeStep = Math.max(0.1, Math.min(10.0, timeStep));
    }

    /**
     * 设置仿真速度
     */
    public void setSpeedMultiplier(double multiplier) {
        this.speedMultiplier = Math.max(0.1, Math.min(10.0, multiplier));
    }

    /**
     * 仿真状态枚举
     */
    public enum SimulationState {
        STOPPED,      // 已停止
        INITIALIZED,  // 已初始化
        RUNNING,      // 运行中
        PAUSED        // 已暂停
    }
}
