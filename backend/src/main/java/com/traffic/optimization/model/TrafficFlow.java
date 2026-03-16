package com.traffic.optimization.model;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import lombok.Data;
import java.util.List;

/**
 * 交通流类 - 代表一组从入口到目的地的车辆
 *
 * @author Chengkun Liao, Mingjie Shen
 */
@Data
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "flowId")
@JsonIgnoreProperties({"path"})
public class TrafficFlow {
    /**
     * 流ID（唯一标识符）
     */
    private String flowId;

    /**
     * 入口节点
     */
    private Node entryPoint;

    /**
     * 目的地节点
     */
    private Node destination;

    /**
     * 车辆数量
     */
    private int numberOfCars;

    /**
     * 旅行时间计数器（秒）
     */
    private double travelTimeCounter;

    /**
     * 路径（节点序列）
     */
    private List<Node> path;

    /**
     * 当前路径索引
     */
    private int currentPathIndex;

    /**
     * 当前所在的边
     */
    private Edge currentEdge;

    /**
     * 在当前边上已行驶的时间（秒）
     */
    private double timeOnCurrentEdge;

    /**
     * 流状态
     */
    private FlowState state;

    /**
     * 已完成的车辆数量
     */
    private int completedCars;

    /**
     * 总行驶距离
     */
    private double totalDistance;

    /**
     * 创建时间（模拟时间）
     */
    private long createdAt;

    /**
     * 构造函数
     */
    public TrafficFlow(String flowId, Node entryPoint, Node destination, int numberOfCars) {
        this.flowId = flowId;
        this.entryPoint = entryPoint;
        this.destination = destination;
        this.numberOfCars = numberOfCars;
        this.travelTimeCounter = 0;
        this.currentPathIndex = 0;
        this.timeOnCurrentEdge = 0;
        this.state = FlowState.WAITING;
        this.completedCars = 0;
        this.totalDistance = 0;
        this.createdAt = System.currentTimeMillis();
    }

    /**
     * 设置路径
     */
    public void setPath(List<Node> path) {
        this.path = path;
        if (path != null && path.size() > 0) {
            this.totalDistance = calculatePathDistance();
        }
    }

    /**
     * 计算路径总距离
     */
    private double calculatePathDistance() {
        double distance = 0;
        for (int i = 0; i < path.size() - 1; i++) {
            Node from = path.get(i);
            Node to = path.get(i + 1);
            Edge edge = from.getEdgeTo(to);
            if (edge != null) {
                distance += edge.getDistance();
            }
        }
        return distance;
    }

    /**
     * 获取当前节点
     */
    public Node getCurrentNode() {
        if (path == null || currentPathIndex >= path.size()) {
            return null;
        }
        return path.get(currentPathIndex);
    }

    /**
     * 获取下一个节点
     */
    public Node getNextNode() {
        if (path == null || currentPathIndex + 1 >= path.size()) {
            return null;
        }
        return path.get(currentPathIndex + 1);
    }

    /**
     * 移动到下一个节点
     */
    public boolean moveToNextNode() {
        if (hasReachedDestination()) {
            return false;
        }

        currentPathIndex++;
        timeOnCurrentEdge = 0;

        if (hasReachedDestination()) {
            state = FlowState.COMPLETED;
            completedCars = numberOfCars;
            return true;
        }

        return true;
    }

    /**
     * 检查是否到达目的地
     */
    public boolean hasReachedDestination() {
        return currentPathIndex >= path.size() - 1;
    }

    /**
     * 检查是否完成
     */
    public boolean isCompleted() {
        return state == FlowState.COMPLETED;
    }

    /**
     * 获取平均速度（km/h）
     */
    public double getAverageSpeed() {
        if (travelTimeCounter == 0) {
            return 0;
        }
        // 距离 / (时间/3600)
        double travelTimeHours = travelTimeCounter / 3600.0;
        return totalDistance / travelTimeHours;
    }

    /**
     * 获取效率指标（用于计算E值）
     * E_i = Ni × Li / ti
     */
    public double getEfficiencyMetric() {
        if (travelTimeCounter == 0 || !isCompleted()) {
            return 0;
        }
        return (numberOfCars * totalDistance) / (travelTimeCounter / 3600.0);
    }

    /**
     * 更新旅行时间（每秒调用）
     */
    public void updateTravelTime(double deltaTime) {
        if (state == FlowState.ACTIVE) {
            travelTimeCounter += deltaTime;
            timeOnCurrentEdge += deltaTime;
        }
    }

    /**
     * 流状态枚举
     */
    public enum FlowState {
        WAITING,    // 等待进入网络
        ACTIVE,     // 在网络中移动
        BLOCKED,    // 被阻塞（道路满或红灯）
        COMPLETED   // 已完成
    }

    @Override
    public String toString() {
        return "TrafficFlow{" +
                "id='" + flowId + '\'' +
                ", from=" + entryPoint.getId() +
                ", to=" + destination.getId() +
                ", cars=" + numberOfCars +
                ", state=" + state +
                ", time=" + String.format("%.1f", travelTimeCounter) + "s" +
                '}';
    }
}
