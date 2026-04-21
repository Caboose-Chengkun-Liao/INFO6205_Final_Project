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
     * 注:两个方向共享该值时视为对称配时;Webster 会通过 greenDurationEW/NS 实现非对称分配
     */
    private int greenDuration;

    /**
     * 东西方向绿灯时长(秒)- 为非对称配时而设
     */
    private int greenDurationEW;

    /**
     * 南北方向绿灯时长(秒)- 为非对称配时而设
     */
    private int greenDurationNS;

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
        this.greenDurationEW = greenDuration;
        this.greenDurationNS = greenDuration;
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
     * 状态机: GREEN -> YELLOW -> ALL_RED -> (switch dir) GREEN -> YELLOW -> ALL_RED -> ...
     *
     * 一个完整周期 = EW(绿+黄+全红) + NS(绿+黄+全红)
     * 没有多余的 RED 过渡态:ALL_RED 结束后直接切换方向进入对向 GREEN。
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
                    // 无全红间隔,直接切到对向 GREEN
                    switchDirection();
                    currentState = SignalState.GREEN;
                    remainingTime = greenForDirection(currentDirection);
                }
                break;
            case ALL_RED:
                switchDirection();
                currentState = SignalState.GREEN;
                remainingTime = greenForDirection(currentDirection);
                break;
            case RED:
                // 只有从 synchronize() 人工置位才可能进入该状态;容错处理切到 GREEN
                currentState = SignalState.GREEN;
                remainingTime = greenForDirection(currentDirection);
                break;
        }
    }

    private int greenForDirection(SignalDirection dir) {
        return dir == SignalDirection.EAST_WEST ? greenDurationEW : greenDurationNS;
    }

    private SignalDirection oppositeDirection(SignalDirection dir) {
        return dir == SignalDirection.EAST_WEST
            ? SignalDirection.NORTH_SOUTH
            : SignalDirection.EAST_WEST;
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
     * 调整绿灯时长(对称:两个方向使用同一值,用于 FIXED/Q-Learning 等不区分方向的场景)
     */
    public void adjustGreenDuration(int newDuration) {
        int clamped = Math.max(10, Math.min(90, newDuration));
        this.greenDuration = clamped;
        this.greenDurationEW = clamped;
        this.greenDurationNS = clamped;
        this.redDuration = greenDuration + yellowDuration + allRedDuration;
    }

    /**
     * 非对称调整两个方向的绿灯时长(用于 Webster 按方向需求分配)
     */
    public void adjustGreenDurations(int ewDuration, int nsDuration) {
        this.greenDurationEW = Math.max(10, Math.min(90, ewDuration));
        this.greenDurationNS = Math.max(10, Math.min(90, nsDuration));
        this.greenDuration = (greenDurationEW + greenDurationNS) / 2;
        // redDuration 不再是恒定值(随方向切换而变);保留字段兼容旧代码但其值仅作参考
        this.redDuration = greenDuration + yellowDuration + allRedDuration;
    }

    /**
     * 获取完整周期时长(秒)
     * 一个完整周期 = EW 绿 + 黄 + 全红 + NS 绿 + 黄 + 全红
     */
    public int getCycleLength() {
        return greenDurationEW + greenDurationNS + (yellowDuration + allRedDuration) * 2;
    }

    /**
     * 按给定偏移量(秒)一次性对齐信号相位 —— 用于绿波协调。
     *
     * 相位时间轴(从"EW 绿开始"为 0):
     *   [0, ewG)                      EW GREEN
     *   [ewG, ewG+y)                  EW YELLOW
     *   [ewG+y, ewG+y+r)              ALL_RED (切 EW→NS)
     *   [ewG+y+r, ewG+y+r+nsG)        NS GREEN
     *   [ewG+y+r+nsG, ewG+y+r+nsG+y)  NS YELLOW
     *   [...+y, cycle)                ALL_RED (切 NS→EW)
     */
    public void synchronize(int offsetSec) {
        int cycle = getCycleLength();
        int t = ((offsetSec % cycle) + cycle) % cycle;
        int ewG = greenDurationEW, nsG = greenDurationNS;
        int y = yellowDuration, r = allRedDuration;

        if (t < ewG) {
            currentDirection = SignalDirection.EAST_WEST;
            currentState = SignalState.GREEN;
            remainingTime = ewG - t;
        } else if (t < ewG + y) {
            currentDirection = SignalDirection.EAST_WEST;
            currentState = SignalState.YELLOW;
            remainingTime = (ewG + y) - t;
        } else if (t < ewG + y + r) {
            currentDirection = SignalDirection.EAST_WEST;
            currentState = SignalState.ALL_RED;
            remainingTime = (ewG + y + r) - t;
        } else if (t < ewG + y + r + nsG) {
            currentDirection = SignalDirection.NORTH_SOUTH;
            currentState = SignalState.GREEN;
            remainingTime = (ewG + y + r + nsG) - t;
        } else if (t < ewG + y + r + nsG + y) {
            currentDirection = SignalDirection.NORTH_SOUTH;
            currentState = SignalState.YELLOW;
            remainingTime = (ewG + y + r + nsG + y) - t;
        } else {
            currentDirection = SignalDirection.NORTH_SOUTH;
            currentState = SignalState.ALL_RED;
            remainingTime = cycle - t;
        }
        this.offset = offsetSec;
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
