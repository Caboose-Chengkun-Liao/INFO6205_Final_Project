package com.traffic.optimization.model;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import lombok.Data;
import java.util.LinkedList;
import java.util.Queue;

/**
 * 边类 - 代表道路
 *
 * @author Chengkun Liao, Mingjie Shen
 */
@Data
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
@JsonIgnoreProperties({"vehicleQueue"})
public class Edge {
    /**
     * 边ID
     */
    private String id;

    /**
     * 起点节点
     */
    private Node fromNode;

    /**
     * 终点节点
     */
    private Node toNode;

    /**
     * 道路距离（公里）
     */
    private double distance;

    /**
     * 道路容量（车辆数/公里）
     */
    private double capacityPerKm;

    /**
     * 速度限制（公里/小时）
     */
    private double speedLimit;

    /**
     * 当前道路上的车辆队列
     */
    private Queue<TrafficFlow> vehicleQueue;

    /**
     * 当前道路上的车辆数量
     */
    private int currentVehicleCount;

    /**
     * 构造函数
     */
    public Edge(String id, Node fromNode, Node toNode, double distance) {
        this(id, fromNode, toNode, distance, 50.0, 60.0);
    }

    /**
     * 完整构造函数
     */
    public Edge(String id, Node fromNode, Node toNode, double distance,
                double capacityPerKm, double speedLimit) {
        this.id = id;
        this.fromNode = fromNode;
        this.toNode = toNode;
        this.distance = distance;
        this.capacityPerKm = capacityPerKm;
        this.speedLimit = speedLimit;
        this.vehicleQueue = new LinkedList<>();
        this.currentVehicleCount = 0;
    }

    /**
     * 获取总容量
     */
    public double getTotalCapacity() {
        return capacityPerKm * distance;
    }

    /**
     * 检查是否已满
     */
    public boolean isFull() {
        return currentVehicleCount >= getTotalCapacity();
    }

    /**
     * 获取当前占用率
     */
    public double getOccupancyRate() {
        return (double) currentVehicleCount / getTotalCapacity();
    }

    /**
     * 获取当前排队长度（vehicleQueue 大小）
     * 单独暴露成 int，避免序列化整个 queue 对象
     */
    public int getQueueLength() {
        return vehicleQueue == null ? 0 : vehicleQueue.size();
    }

    /**
     * BPR (Bureau of Public Roads) 速度-流量模型
     *
     * 公式: speed = freeFlowSpeed / (1 + α × (V/C)^β)
     * - α = 0.15 (标准参数，控制拥堵灵敏度)
     * - β = 4.0  (标准参数，控制速度衰减的陡峭程度)
     * - V/C = volume/capacity 比 (即 occupancyRate)
     *
     * 相比原来的阶梯式衰减，BPR 提供平滑连续的速度变化，
     * 更符合真实交通流理论 (Highway Capacity Manual)
     */
    private static final double BPR_ALPHA = 0.15;
    private static final double BPR_BETA = 4.0;
    private static final double MIN_SPEED_RATIO = 0.1; // 最低速度不低于限速的10%

    public double getActualSpeed() {
        double vcRatio = getOccupancyRate(); // V/C ratio

        // BPR 公式
        double speedReduction = 1.0 + BPR_ALPHA * Math.pow(vcRatio, BPR_BETA);
        double actualSpeed = speedLimit / speedReduction;

        // 确保最低速度
        return Math.max(actualSpeed, speedLimit * MIN_SPEED_RATIO);
    }

    /**
     * 计算理想通行时间（分钟）- 不考虑拥堵
     * 增加2倍让车辆移动更慢，更容易观察
     */
    public double getIdealTravelTime() {
        return (distance / speedLimit) * 60 * 2;
    }

    /**
     * 计算实际通行时间（分钟）- 考虑拥堵影响
     * 增加2倍让车辆移动更慢，更容易观察
     */
    public double getActualTravelTime() {
        double actualSpeed = getActualSpeed();
        if (actualSpeed == 0) {
            return Double.MAX_VALUE; // 避免除零
        }
        return (distance / actualSpeed) * 60 * 2;
    }

    /**
     * 添加车辆到道路
     */
    public boolean addVehicle(TrafficFlow flow) {
        if (isFull()) {
            return false;
        }
        vehicleQueue.offer(flow);
        currentVehicleCount += flow.getNumberOfCars();
        return true;
    }

    /**
     * 移除指定的交通流从道路（确保移除正确的 flow）
     */
    public boolean removeVehicle(TrafficFlow flow) {
        boolean removed = vehicleQueue.remove(flow);
        if (removed) {
            currentVehicleCount = Math.max(0, currentVehicleCount - flow.getNumberOfCars());
        }
        return removed;
    }

    /**
     * 移除队首车辆从道路（兼容旧接口）
     */
    public TrafficFlow removeVehicle() {
        TrafficFlow flow = vehicleQueue.poll();
        if (flow != null) {
            currentVehicleCount = Math.max(0, currentVehicleCount - flow.getNumberOfCars());
        }
        return flow;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Edge edge = (Edge) o;
        return id.equals(edge.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "Edge{" +
                "id='" + id + '\'' +
                ", from=" + fromNode.getId() +
                ", to=" + toNode.getId() +
                ", distance=" + distance + "km" +
                ", vehicles=" + currentVehicleCount + "/" + getTotalCapacity() +
                '}';
    }
}
