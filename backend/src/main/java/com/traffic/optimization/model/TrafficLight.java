package com.traffic.optimization.model;

import lombok.Data;

/**
 * 交通信号灯类 - 支持多相位、全红间隔和可配置周期
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
     * 当前信号状态（红灯/绿灯/黄灯/全红）
     */
    private SignalState currentState;

    /**
     * 绿灯时长（秒）- 可通过优化算法动态调整
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
     * 全红间隔时长（秒）- 所有方向红灯的安全间隔
     */
    private int allRedDuration;

    /**
     * 当前状态剩余时间（秒）
     */
    private int remainingTime;

    /**
     * 信号偏移量（秒）- 用于绿波协调
     */
    private int offset;

    /**
     * 累计已服务车辆数（用于效率分析）
     */
    private long totalServedVehicles;

    /**
     * 构造函数 - 默认配置
     */
    public TrafficLight(String id) {
        this(id, 30, 3, 2);
    }

    /**
     * 构造函数 - 自定义配置
     *
     * @param id            信号灯ID
     * @param greenDuration 绿灯时长（秒）
     * @param yellowDuration 黄灯时长（秒）
     * @param allRedDuration 全红间隔时长（秒）
     */
    public TrafficLight(String id, int greenDuration, int yellowDuration, int allRedDuration) {
        this.id = id;
        this.currentDirection = SignalDirection.EAST_WEST;
        this.currentState = SignalState.GREEN;
        this.greenDuration = greenDuration;
        this.yellowDuration = yellowDuration;
        this.allRedDuration = allRedDuration;
        // 红灯时长 = 对向绿灯 + 黄灯 + 全红
        this.redDuration = greenDuration + yellowDuration + allRedDuration;
        this.remainingTime = greenDuration;
        this.offset = 0;
        this.totalServedVehicles = 0;
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
     * 状态机: GREEN -> YELLOW -> ALL_RED -> RED -> GREEN
     */
    private void switchState() {
        switch (currentState) {
            case GREEN:
                currentState = SignalState.YELLOW;
                remainingTime = yellowDuration;
                break;
            case YELLOW:
                if (allRedDuration > 0) {
                    currentState = SignalState.ALL_RED;
                    remainingTime = allRedDuration;
                } else {
                    // 无全红间隔，直接切换
                    currentState = SignalState.RED;
                    remainingTime = redDuration;
                    switchDirection();
                }
                break;
            case ALL_RED:
                currentState = SignalState.RED;
                remainingTime = redDuration;
                switchDirection();
                break;
            case RED:
                currentState = SignalState.GREEN;
                remainingTime = greenDuration;
                break;
        }
    }

    /**
     * 切换通行方向
     */
    private void switchDirection() {
        currentDirection = (currentDirection == SignalDirection.EAST_WEST)
            ? SignalDirection.NORTH_SOUTH
            : SignalDirection.EAST_WEST;
    }

    /**
     * 检查指定方向是否可以通行
     * 绿灯可通行，黄灯也可通行（模拟实际中的黄灯通行）
     */
    public boolean canPass(SignalDirection direction) {
        if (currentDirection != direction) {
            return false;
        }
        return currentState == SignalState.GREEN || currentState == SignalState.YELLOW;
    }

    /**
     * 调整绿灯时长（用于优化算法）
     */
    public void adjustGreenDuration(int newDuration) {
        this.greenDuration = Math.max(10, Math.min(90, newDuration)); // 限制在10-90秒
        this.redDuration = greenDuration + yellowDuration + allRedDuration;
    }

    /**
     * 获取完整周期时长（秒）
     * 一个完整周期 = (绿灯 + 黄灯 + 全红) × 2 个方向
     */
    public int getCycleLength() {
        return (greenDuration + yellowDuration + allRedDuration) * 2;
    }

    /**
     * 记录服务的车辆数
     */
    public void recordServedVehicles(int count) {
        this.totalServedVehicles += count;
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
        RED,     // 红灯
        YELLOW,  // 黄灯
        GREEN,   // 绿灯
        ALL_RED  // 全红（安全间隔）
    }

    @Override
    public String toString() {
        return "TrafficLight{" +
                "id='" + id + '\'' +
                ", direction=" + currentDirection +
                ", state=" + currentState +
                ", remaining=" + remainingTime + "s" +
                ", cycle=" + getCycleLength() + "s" +
                '}';
    }
}
