# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概述

这是一个交通信号灯优化项目（INFO6205课程期末项目），通过优化十字路口的信号灯控制来提高交通效率。项目作者：Chengkun Liao 和 Mingjie Shen。

**核心目标**：基于真实地图数据模拟交通流和信号灯控制，通过优化信号灯时序来最大化交通吞吐量。

## 核心数据结构

### 加权有向图（Weighted Directed Graph）
- **节点（Nodes）**：代表十字路口/交叉点，配有信号灯
  - 蓝色标记节点（1, 2, 3...）：内部路口
  - 红色标记节点（A, B, C...）：边界出入口
- **边（Edges）**：代表道路，权重为道路距离
- **假设条件**：
  - 所有道路的单位容量（n cars/km）相同
  - 所有道路的速度限制相同
  - 道路容量仅与距离相关

### 交通流对象（Traffic Flow）
每个流对象包含以下属性：
- `flowID`：唯一标识符
- `entryPoint`：入口点
- `destination`：目的地
- `numberOfCars`：车辆数量
- `travelTimeCounter`：旅行时间计数器

**路由策略**：所有交通流使用最短路径算法（如 Dijkstra）在入口和目的地之间行驶。

## 系统架构

### 1. 道路网络层（Road Network Layer）
- 实现加权有向图
- 管理节点（路口）和边（道路）的关系
- 存储道路距离、容量等属性

### 2. 交通流模拟层（Traffic Flow Simulation Layer）
- 创建和管理交通流对象
- 实现最短路径算法计算路由
- 追踪每个流的实时位置和状态
- 模拟车辆在路口的排队和通过

### 3. 信号控制层（Signal Control Layer）
- 管理每个路口的信号灯状态
- 根据交通流量动态调整信号时序
- 实现信号优化算法

### 4. 性能评估层（Performance Evaluation Layer）
计算交通效率指标 E（每小时评估一次）：

```
E = Σ(Ni × Li / ti) / Σ(Ni)
```

其中：
- E：效率值
- Ni：流 i 中的车辆数量
- Li：道路长度
- ti：流通过两个路口的时间

**优化目标**：通过调整信号灯控制的 ti 值来最大化 E

## 开发规范

### 代码组织建议
```
src/
├── graph/              # 图结构实现
│   ├── Node.java       # 路口节点
│   ├── Edge.java       # 道路边
│   └── Graph.java      # 图主类
├── traffic/            # 交通流模拟
│   ├── TrafficFlow.java
│   └── FlowManager.java
├── signal/             # 信号控制
│   ├── TrafficLight.java
│   └── SignalController.java
├── algorithm/          # 算法实现
│   ├── ShortestPath.java
│   └── SignalOptimizer.java
├── simulation/         # 仿真引擎
│   └── Simulator.java
└── metrics/            # 性能指标
    └── EfficiencyCalculator.java
```

### 关键实现要点

1. **最短路径算法**：实现 Dijkstra 算法用于交通流路由计算

2. **时间步进仿真**：
   - 使用离散事件模拟或固定时间步长
   - 每个时间步更新所有交通流位置
   - 处理路口信号灯状态变化

3. **信号优化逻辑**：
   - 需要考虑不同时段（特别是高峰时段）的交通模式
   - 基于实时/历史交通数据调整信号时序
   - 平衡多个路口的协调控制

4. **效率计算**：
   - 每小时统计所有交通流的 Ni、Li、ti
   - 计算综合效率指标 E
   - 记录和比较不同控制策略下的效率

### 测试建议

- **单元测试**：图结构操作、最短路径算法、效率计算公式
- **集成测试**：交通流在网络中的完整路径模拟
- **性能测试**：大规模交通流下的系统响应
- **场景测试**：高峰时段、低峰时段等不同交通模式

## 项目状态

**当前阶段**：项目提案阶段，尚未开始代码实现

**已完成**：
- 问题定义和需求分析
- 数据结构设计
- 架构规划

**待完成**：
- 编程语言/框架选择
- 构建系统配置
- 核心代码实现
- 测试框架搭建
