package com.traffic.optimization.model;

import lombok.Data;

/**
 * 交通信号灯类
 *
 * @author Chengkun Liao, Mingjie Shen
 */
@Data
public class TrafficLight {
    /**
     * 信号灯ID（对应节点ID）
     */
    private String id;

    /**
     * 当前信号方向（东西/南北）
     */
    private SignalDirection currentDirection;

    /**
     * 当前信号状态（红灯/绿灯/黄灯）
     */
    private SignalState currentState;

    /**
     * 绿灯时长（秒）
     */
    private int greenDuration;

    /**
     * 黄灯时长（秒）
     */
    private int yellowDuration;

    /**
     * 红灯时长（秒）
     */
    private int redDuration;

    /**
     * 当前状态剩余时间（秒）
     */
    private int remainingTime;

    /**
     * 构造函数 - 默认配置
     */
    public TrafficLight(String id) {
        this.id = id;
        this.currentDirection = SignalDirection.EAST_WEST;
        this.currentState = SignalState.GREEN;
        this.greenDuration = 30;
        this.yellowDuration = 3;
        this.redDuration = 33; // 另一方向的绿灯+黄灯时间
        this.remainingTime = greenDuration;
    }

    /**
     * 更新信号灯状态（每秒调用）
     */
    public void update() {
        remainingTime--;

        if (remainingTime <= 0) {
            switchState();
        }
    }

    /**
     * 切换信号状态
     */
    private void switchState() {
        switch (currentState) {
            case GREEN:
                currentState = SignalState.YELLOW;
                remainingTime = yellowDuration;
                break;
            case YELLOW:
                currentState = SignalState.RED;
                remainingTime = redDuration;
                // 切换方向
                currentDirection = (currentDirection == SignalDirection.EAST_WEST)
                    ? SignalDirection.NORTH_SOUTH
                    : SignalDirection.EAST_WEST;
                break;
            case RED:
                currentState = SignalState.GREEN;
                remainingTime = greenDuration;
                break;
        }
    }

    /**
     * 检查指定方向是否可以通行
     */
    public boolean canPass(SignalDirection direction) {
        return currentDirection == direction && currentState == SignalState.GREEN;
    }

    /**
     * 调整绿灯时长（用于优化）
     */
    public void adjustGreenDuration(int newDuration) {
        this.greenDuration = Math.max(10, Math.min(60, newDuration)); // 限制在10-60秒
        this.redDuration = greenDuration + yellowDuration;
    }

    /**
     * 信号方向枚举
     */
    public enum SignalDirection {
        EAST_WEST,  // 东西方向
        NORTH_SOUTH // 南北方向
    }

    /**
     * 信号状态枚举
     */
    public enum SignalState {
        RED,    // 红灯
        YELLOW, // 黄灯
        GREEN   // 绿灯
    }

    @Override
    public String toString() {
        return "TrafficLight{" +
                "id='" + id + '\'' +
                ", direction=" + currentDirection +
                ", state=" + currentState +
                ", remaining=" + remainingTime + "s" +
                '}';
    }
}
