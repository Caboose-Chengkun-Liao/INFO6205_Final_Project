package com.traffic.optimization.model;

/**
 * 节点类型枚举
 */
public enum NodeType {
    /**
     * 内部路口 - 有信号灯控制
     */
    INTERSECTION,

    /**
     * 边界出入口 - 交通流的起点和终点
     */
    BOUNDARY
}
