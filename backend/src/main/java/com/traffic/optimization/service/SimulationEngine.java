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
     * 是否启用持续车流生成
     */
    private boolean continuousFlowEnabled;

    /**
     * 车流生成间隔（秒）
     */
    private long flowGenerationInterval;

    /**
     * 上次生成车流时间
     */
    private long lastFlowGenerationTime;

    /**
     * 随机数生成器
     */
    private Random random;

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
        this.continuousFlowEnabled = true; // 默认启用持续车流
        this.flowGenerationInterval = 30; // 每30秒生成一次新车流
        this.lastFlowGenerationTime = 0;
        this.random = new Random();
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

            // 立即生成初始车流，让用户马上看到车辆
            if (continuousFlowEnabled && state == SimulationState.RUNNING) {
                System.out.println("生成初始车流...");
                generateRandomFlows();
                generateRandomFlows(); // 生成两批初始车流
            }

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
        System.out.println("=== DEBUG: reset() 被调用 ===");
        System.out.println("调用堆栈：");
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        for (int i = 2; i < Math.min(stackTrace.length, 8); i++) {
            System.out.println("  " + stackTrace[i]);
        }

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

        // 5. 持续生成新车流（如果启用）
        if (continuousFlowEnabled && currentTime - lastFlowGenerationTime >= flowGenerationInterval) {
            generateRandomFlows();
            lastFlowGenerationTime = currentTime;
        }

        // 6. 定期评估效率
        if (currentTime - lastEfficiencyEvaluationTime >= efficiencyEvaluationInterval) {
            evaluateEfficiency();
            lastEfficiencyEvaluationTime = currentTime;
        }

        // 7. 定期优化信号灯
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
            // 跳过已完成的流
            if (flow.getState() == TrafficFlow.FlowState.COMPLETED) {
                System.out.println("DEBUG updateTrafficFlows: 跳过已完成的流 " + flow.getFlowId());
                continue;
            }

            if (flow.getState() == TrafficFlow.FlowState.WAITING) {
                // 尝试进入网络
                tryEnterNetwork(flow);
            } else if (flow.getState() == TrafficFlow.FlowState.ACTIVE) {
                // 尝试移动到下一个节点
                tryMoveToNextNode(flow);
            } else if (flow.getState() == TrafficFlow.FlowState.BLOCKED) {
                // 被阻塞的车辆也需要继续尝试移动（信号灯可能变绿了）
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

        System.out.println("DEBUG tryEnterNetwork: " + flow.getFlowId() +
            " currentNode=" + (currentNode != null ? currentNode.getId() : "null") +
            " nextNode=" + (nextNode != null ? nextNode.getId() : "null"));

        if (nextNode == null) {
            System.out.println("DEBUG: nextNode is null for " + flow.getFlowId());
            return;
        }

        Edge edge = currentNode.getEdgeTo(nextNode);
        System.out.println("DEBUG: edge=" + (edge != null ? edge.getId() : "null") +
            " isFull=" + (edge != null ? edge.isFull() : "N/A"));

        if (edge != null && !edge.isFull()) {
            // 检查信号灯（如果当前节点是路口）
            boolean canPass = canPass(currentNode, nextNode);
            System.out.println("DEBUG: canPass=" + canPass);

            if (canPass) {
                boolean added = edge.addVehicle(flow);
                System.out.println("DEBUG: addVehicle result=" + added);
                if (added) {
                    flow.setCurrentEdge(edge);
                    flow.setState(TrafficFlow.FlowState.ACTIVE);
                    System.out.println("交通流 " + flow.getFlowId() + " 成功进入道路 " + edge.getId());
                }
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

        // 检查是否已在当前边上停留足够时间（使用实际速度，考虑拥堵）
        double requiredTime = currentEdge.getActualTravelTime() * 60; // 转换为秒
        double occupancyRate = currentEdge.getOccupancyRate();
        System.out.println("DEBUG tryMoveToNextNode: " + flow.getFlowId() +
            " timeOnEdge=" + flow.getTimeOnCurrentEdge() +
            " requiredTime=" + requiredTime +
            " occupancy=" + String.format("%.1f%%", occupancyRate * 100) +
            " actualSpeed=" + String.format("%.1f", currentEdge.getActualSpeed()) + "km/h" +
            " state=" + flow.getState());

        if (flow.getTimeOnCurrentEdge() >= requiredTime) {
            // 尝试移动到下一个节点
            Node currentNode = flow.getCurrentNode();
            Node nextNode = flow.getNextNode();

            boolean canPassResult = (nextNode != null && canPass(currentNode, nextNode));
            System.out.println("DEBUG: currentNode=" + (currentNode != null ? currentNode.getId() : "null") +
                " nextNode=" + (nextNode != null ? nextNode.getId() : "null") +
                " canPass=" + canPassResult);

            if (canPassResult) {
                // 可以通过信号灯，从当前边移除
                currentEdge.removeVehicle();

                // 移动到下一个节点
                flow.moveToNextNode();
                flow.setTimeOnCurrentEdge(0); // 重置时间计数器

                // 检查是否到达目的地
                if (flow.hasReachedDestination()) {
                    flow.setState(TrafficFlow.FlowState.COMPLETED);
                    System.out.println("交通流 " + flow.getFlowId() + " 已完成旅程！");
                    System.out.println("DEBUG: 流状态已设置为 COMPLETED, isCompleted=" + flow.isCompleted() +
                        " currentPathIndex=" + flow.getCurrentPathIndex() + " pathSize=" + flow.getPath().size());
                } else {
                    // 还有下一段路，尝试进入下一条边
                    Node newNextNode = flow.getNextNode();
                    if (newNextNode != null) {
                        Edge newEdge = flow.getCurrentNode().getEdgeTo(newNextNode);
                        if (newEdge != null && !newEdge.isFull()) {
                            newEdge.addVehicle(flow);
                            flow.setCurrentEdge(newEdge);
                            flow.setState(TrafficFlow.FlowState.ACTIVE);
                        } else {
                            // 下一条边已满，暂时阻塞
                            flow.setState(TrafficFlow.FlowState.BLOCKED);
                        }
                    } else {
                        // 没有下一个节点了，说明到达目的地
                        flow.setState(TrafficFlow.FlowState.COMPLETED);
                        System.out.println("交通流 " + flow.getFlowId() + " 已到达目的地！");
                    }
                }
            } else {
                // 无法通过（信号灯是红灯或方向不匹配），设置为阻塞状态
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
     * 随机生成新的车流
     */
    private void generateRandomFlows() {
        if (graph == null) {
            return;
        }

        // 获取所有边界节点
        List<Node> boundaryNodes = graph.getBoundaryNodes();
        if (boundaryNodes.size() < 2) {
            return;
        }

        // 随机生成1-3个车流
        int flowCount = random.nextInt(3) + 1;

        for (int i = 0; i < flowCount; i++) {
            // 随机选择不同的入口和出口
            Node entry = boundaryNodes.get(random.nextInt(boundaryNodes.size()));
            Node destination;
            do {
                destination = boundaryNodes.get(random.nextInt(boundaryNodes.size()));
            } while (entry.equals(destination));

            // 随机车辆数量：5-15辆
            int numberOfCars = random.nextInt(11) + 5;

            try {
                flowManager.createFlow(entry.getId(), destination.getId(), numberOfCars);
                System.out.println("自动生成车流: " + entry.getId() + " → " +
                    destination.getId() + " (" + numberOfCars + "辆)");
            } catch (Exception e) {
                System.err.println("生成车流失败: " + e.getMessage());
            }
        }
    }

    /**
     * 设置是否启用持续车流生成
     */
    public void setContinuousFlowEnabled(boolean enabled) {
        this.continuousFlowEnabled = enabled;
        System.out.println("持续车流生成: " + (enabled ? "启用" : "禁用"));
    }

    /**
     * 设置车流生成间隔
     */
    public void setFlowGenerationInterval(long interval) {
        this.flowGenerationInterval = Math.max(10, interval); // 最小10秒
        System.out.println("车流生成间隔设置为: " + interval + "秒");
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
