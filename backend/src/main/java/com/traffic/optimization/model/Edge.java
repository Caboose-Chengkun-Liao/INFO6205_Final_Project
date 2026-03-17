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
     * 获取实际速度（考虑拥堵影响）
     * 根据道路占用率动态调整速度
     */
    public double getActualSpeed() {
        double occupancyRate = getOccupancyRate();

        if (occupancyRate >= 0.9) {
            // 严重拥堵：速度降至30%
            return speedLimit * 0.3;
        } else if (occupancyRate >= 0.75) {
            // 高拥堵：速度降至50%
            return speedLimit * 0.5;
        } else if (occupancyRate >= 0.5) {
            // 中等拥堵：速度降至75%
            return speedLimit * 0.75;
        } else if (occupancyRate >= 0.25) {
            // 轻微拥堵：速度降至90%
            return speedLimit * 0.9;
        }

        // 畅通：正常速度
        return speedLimit;
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
     * 移除车辆从道路
     */
    public TrafficFlow removeVehicle() {
        TrafficFlow flow = vehicleQueue.poll();
        if (flow != null) {
            currentVehicleCount -= flow.getNumberOfCars();
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
