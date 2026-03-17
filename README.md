# 🚦 交通信号优化系统 (Traffic Signal Optimization System)

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Java](https://img.shields.io/badge/Java-18-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.0-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-18-blue.svg)](https://reactjs.org/)

**INFO6205 Program Structure and Algorithms - Final Project**

**作者**: Chengkun Liao, Mingjie Shen

**学期**: Spring 2025 | **学校**: Northeastern University

---

## 📖 目录

- [项目简介](#项目简介)
- [系统特性](#系统特性)
- [技术栈](#技术栈)
- [快速开始](#快速开始)
- [项目结构](#项目结构)
- [核心模块](#核心模块)
- [API文档](#api文档)
- [效率计算公式](#效率计算公式)
- [文档导航](#文档导航)
- [演示截图](#演示截图)
- [贡献指南](#贡献指南)
- [许可证](#许可证)

---

## 项目简介

交通信号优化系统是一个**企业级交通仿真与优化平台**，基于美国弗吉尼亚州阿灵顿市的真实路网数据构建。系统通过智能算法优化信号灯控制，提升城市交通效率，减少拥堵和等待时间。

### 核心价值

- 🚦 **智能信号控制** - 三种优化模式（固定/自适应/智能），动态调整信号时序
- 📊 **实时性能监控** - 8项关键KPI指标，全方位评估交通状态
- 🗺️ **交互式可视化** - SVG地图渲染，实时车辆动画，热力图叠加
- 📈 **数据驱动决策** - 导出CSV/JSON数据，支持深度分析
- 🎯 **科学评估体系** - 基于效率公式的量化评估方法

### 应用场景

- 🏙️ **城市交通规划** - 评估不同信号控制策略对交通流的影响
- 📚 **教学研究** - 算法与数据结构课程的实践项目
- 🔬 **学术研究** - 交通工程、智能交通系统研究
- 💡 **决策支持** - 为交通管理部门提供优化建议

---

## 系统特性

### ✅ 已完成功能 (v1.0)

#### 后端 (Spring Boot 3.2.0 + Java 18)
- ✅ **图数据结构** - 加权有向图表示路网 (20 节点, 48 条边)
- ✅ **最短路径算法** - Dijkstra算法计算最优路由
- ✅ **交通流仿真引擎** - 离散事件仿真，支持多车辆并发
- ✅ **动态速度调整** - 基于道路占用率的拥堵减速模拟
  - 占用率 > 90%: 速度降至 30% (严重拥堵)
  - 占用率 > 75%: 速度降至 50% (高拥堵)
  - 占用率 > 50%: 速度降至 75% (中等拥堵)
  - 占用率 > 25%: 速度降至 90% (轻微拥堵)
- ✅ **信号灯控制系统** - FIXED / ADAPTIVE / INTELLIGENT 三种模式
- ✅ **性能指标计算** - 效率、速度、拥堵度等8项KPI
- ✅ **WebSocket实时推送** - SockJS + STOMP协议，低延迟数据传输
- ✅ **RESTful API** - 完整的CRUD接口，支持跨域访问
- ✅ **JSON序列化优化** - 防止循环引用，提升性能

#### 前端 (React 18 + Vite)
- ✅ **地图可视化 (MapVisualization)** - 优化的 SVG 渲染 (1600x800px)
  - 专为桌面端优化，移除 mobile 响应式设计
  - 全宽布局，完整展示阿灵顿路网
  - 实时车辆动画，流畅的视觉效果
- ✅ **控制面板 (ControlPanel)** - 仿真启动/暂停/重置
- ✅ **交通流管理 (TrafficFlowPanel)** - 创建车辆流，表单验证
- ✅ **性能监控 (PerformanceMonitor)** - 实时KPI展示，趋势图表
- ✅ **信号控制 (SignalControlPanel)** - 模式切换，状态监控
- ✅ **节点搜索 (NodeSearchPanel)** - 快速定位，类型筛选
- ✅ **统计仪表板 (StatisticsDashboard)** - 8项KPI卡片，性能摘要
- ✅ **数据导出 (DataExportPanel)** - CSV/JSON格式，5种数据类型
- ✅ **热力图叠加 (TrafficHeatmapOverlay)** - 拥堵/速度/流量 三种模式

### 🔄 实时更新机制
- **WebSocket推送** - 仿真状态、指标数据 (< 50ms延迟)
- **轮询更新** - 效率趋势 (5秒)、热力图数据 (2秒)
- **HMR热更新** - 前端代码修改即时生效

### 🎨 用户体验
- **浮动面板设计** - 9个功能模块，独立展开/收起
- **桌面端优化** - 节点悬停、点击选择、完整地图展示
- **视觉反馈** - 颜色编码（绿/黄/红），动画过渡
- **数据可视化** - 折线图、进度条、热力图
- **拥堵动态模拟** - 实时显示道路占用率与车速变化

---

## 技术栈

### 后端技术

| 技术 | 版本 | 用途 |
|------|------|------|
| **Java** | 18 | 核心开发语言 (必须18，不支持25) |
| **Spring Boot** | 3.2.0 | Web应用框架 |
| **Spring Web** | 6.1.1 | RESTful API |
| **Spring WebSocket** | 6.1.1 | 实时双向通信 |
| **Lombok** | 1.18.30 | 代码简化 |
| **Jackson** | 2.15.3 | JSON序列化 |
| **Maven** | 3.6+ | 项目构建 |

**关键配置**:
```xml
<!-- Jackson循环引用解决方案 -->
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
@JsonIgnoreProperties({"trafficLight", "incomingEdges", "outgoingEdges"})
```

### 前端技术

| 技术 | 版本 | 用途 |
|------|------|------|
| **React** | 18.2.0 | UI框架 |
| **Vite** | 5.0.0 | 构建工具 + HMR |
| **Axios** | 1.6.2 | HTTP客户端 |
| **SockJS Client** | 1.6.1 | WebSocket客户端 |
| **STOMP.js** | 2.3.3 | WebSocket消息协议 |
| **CSS-in-JS** | - | 内联样式系统 |

**开发体验**:
- ⚡ Vite快速构建 (< 1s启动)
- 🔥 热模块替换 (HMR)
- 🎯 组件化架构
- 📦 代码分割 (未来优化)

### 数据结构与算法

```java
// 加权有向图
Graph {
  List<Node> nodes;      // 20个节点 (12路口 + 8边界)
  List<Edge> edges;      // 48条有向边
}

// Dijkstra最短路径
class DijkstraAlgorithm {
  Map<String, Double> distances;
  Map<String, String> previous;
  PriorityQueue<Node> queue;
}

// 效率计算
E = Σ(Ni × Li / ti) / Σ(Ni)
```

---

## 快速开始

### 系统要求

**软件环境**:
- ☕ **Java 18** (JDK 18.0.2.1) - 必须，Lombok不支持Java 25
- 📦 **Node.js** 16+ (推荐 18.x)
- 🔧 **Maven** 3.6+
- 🌐 **浏览器** Chrome 90+ / Firefox 88+ / Safari 14+ / Edge 90+

**硬件要求**:
- CPU: 双核 2.0 GHz+
- 内存: 4 GB RAM (推荐 8 GB)
- 磁盘: 500 MB 可用空间

### 安装步骤

#### 1️⃣ 克隆项目

```bash
git clone https://github.com/your-username/INFO6205_Final_Project.git
cd INFO6205_Final_Project
```

#### 2️⃣ 启动后端服务

```bash
cd backend

# 确保使用Java 18
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-18.0.2.1.jdk/Contents/Home
java -version  # 验证版本

# 编译并运行
mvn clean install
mvn spring-boot:run -DskipTests
```

**成功标志**:
```
2025-03-15 10:30:00.123  INFO 12345 --- [main] c.t.o.TrafficOptimizationApplication : Started TrafficOptimizationApplication in 5.234 seconds (JVM running for 5.678)
```

后端服务运行在 **http://localhost:8080**

#### 3️⃣ 启动前端应用

打开新终端窗口:

```bash
cd frontend

# 安装依赖 (首次运行)
npm install

# 启动开发服务器
npm run dev
```

**成功标志**:
```
  VITE v5.0.0  ready in 1234 ms

  ➜  Local:   http://localhost:5174/
  ➜  Network: use --host to expose
  ➜  press h to show help
```

前端应用运行在 **http://localhost:5174**

#### 4️⃣ 访问应用

在浏览器中打开 `http://localhost:5174`

**连接状态检查**:
- 右上角显示 **🟢 已连接** → WebSocket连接成功
- 右上角显示 **⚪ 未连接** → 检查后端服务是否运行

---

## 项目结构

```
INFO6205_Final_Project/
├── 📄 README.md                     # 项目说明 (本文件)
├── 📄 ROADMAP.md                    # 扩展规划路线图
├── 📄 USER_GUIDE.md                 # 用户使用说明书
├── 📄 SETUP_GUIDE.md                # 环境配置指南
├── 📄 ARLINGTON_MAP.md              # 阿灵顿路网数据
├── 📄 CLAUDE.md                     # AI助手指引文件
│
├── backend/                         # 后端项目 (Spring Boot)
│   ├── src/main/java/com/traffic/optimization/
│   │   ├── 📦 algorithm/            # 算法实现
│   │   │   └── DijkstraAlgorithm.java
│   │   ├── 📦 config/               # 配置类
│   │   │   ├── GraphConfig.java     # 路网数据初始化
│   │   │   ├── WebSocketConfig.java # WebSocket配置
│   │   │   └── CorsConfig.java      # CORS配置
│   │   ├── 📦 controller/           # REST API控制器
│   │   │   └── SimulationController.java
│   │   ├── 📦 model/                # 数据模型
│   │   │   ├── Node.java            # 节点
│   │   │   ├── Edge.java            # 边
│   │   │   ├── TrafficFlow.java     # 交通流
│   │   │   ├── Vehicle.java         # 车辆
│   │   │   ├── TrafficLight.java    # 信号灯
│   │   │   └── SimulationMetrics.java # 指标
│   │   ├── 📦 service/              # 业务逻辑
│   │   │   ├── GraphService.java
│   │   │   ├── SimulationService.java
│   │   │   ├── TrafficFlowService.java
│   │   │   └── MetricsService.java
│   │   ├── 📦 websocket/            # WebSocket处理
│   │   │   └── SimulationWebSocketHandler.java
│   │   └── TrafficOptimizationApplication.java
│   ├── src/main/resources/
│   │   └── application.properties
│   ├── pom.xml                      # Maven依赖配置
│   └── target/                      # 编译输出
│
├── frontend/                        # 前端项目 (React + Vite)
│   ├── src/
│   │   ├── 📦 components/           # React组件
│   │   │   ├── MapVisualization.jsx        # 地图可视化
│   │   │   ├── ControlPanel.jsx            # 仿真控制
│   │   │   ├── TrafficFlowPanel.jsx        # 交通流管理
│   │   │   ├── PerformanceMonitor.jsx      # 性能监控
│   │   │   ├── SignalControlPanel.jsx      # 信号控制
│   │   │   ├── NodeSearchPanel.jsx         # 节点搜索
│   │   │   ├── StatisticsDashboard.jsx     # 统计仪表板
│   │   │   ├── DataExportPanel.jsx         # 数据导出
│   │   │   ├── TrafficHeatmapOverlay.jsx   # 热力图
│   │   │   ├── MetricsDisplay.jsx          # 指标展示
│   │   │   └── MetricsChart.jsx            # 图表
│   │   ├── 📦 services/             # API服务
│   │   │   ├── api.js               # HTTP请求
│   │   │   └── websocket.js         # WebSocket客户端
│   │   ├── App.jsx                  # 主应用组件
│   │   ├── App.css                  # 样式
│   │   └── main.jsx                 # 入口文件
│   ├── public/                      # 静态资源
│   ├── package.json                 # npm依赖配置
│   ├── vite.config.js               # Vite配置
│   └── dist/                        # 生产构建输出
│
└── .gitignore
```

---

## 核心模块

### 1. 地图可视化 (MapVisualization)

**功能**: 交互式SVG地图，实时展示路网和车辆

**技术特点**:
- SVG渲染 (1400×900 viewBox)
- 缩放/平移交互
- 车辆动画 (CSS transition)
- 节点/边悬停提示

**代码示例**:
```jsx
<svg viewBox="0 0 1400 900">
  {/* 渲染边 */}
  {edges.map(edge => (
    <line x1={edge.fromNode.x} y1={edge.fromNode.y}
          x2={edge.toNode.x} y2={edge.toNode.y} />
  ))}

  {/* 渲染车辆 */}
  {vehicles.map(vehicle => (
    <circle cx={vehicle.x} cy={vehicle.y} r={5} fill="#10B981" />
  ))}
</svg>
```

---

### 2. 统计仪表板 (StatisticsDashboard)

**8项关键指标**:

| 指标 | 英文 | 公式/说明 | 目标值 |
|------|------|-----------|--------|
| ⚡ 网络效率 | Network Efficiency | E = Σ(Ni×Li/ti) / ΣNi | ≥40 km/h |
| 🏃 平均速度 | Average Speed | 所有车辆速度均值 | ≥50 km/h |
| 🚗 活跃车辆 | Active Vehicles | 当前路网车辆总数 | - |
| ✅ 完成旅程 | Completed Journeys | 到达目的地车辆数 | - |
| 🛣️ 总行驶距离 | Total Distance | 累计行驶里程 (km) | - |
| ⏱️ 平均行程时间 | Avg Travel Time | 平均旅行时长 (分钟) | 越低越好 |
| 🚦 网络拥堵度 | Network Congestion | 路段容量利用率均值 (%) | <40% |
| 📊 系统吞吐量 | System Throughput | 车辆数×速度/60 (辆/分) | - |

**实时更新**:
- WebSocket推送: `/topic/simulation`
- 轮询间隔: 3秒
- 颜色编码: 🟢良好 🟡中等 🔴较差

---

### 3. 热力图叠加 (TrafficHeatmapOverlay)

**三种可视化模式**:

#### 🚦 拥堵模式 (Congestion)
```
拥堵率 = (当前车辆数 / 路段容量) × 100%

颜色映射:
🟢 <25%  - 畅通
🟢 25-50% - 轻度拥堵
🟡 50-70% - 中度拥堵
🔴 70-85% - 重度拥堵
🔴 >85%  - 严重拥堵
```

#### 🏃 速度模式 (Speed)
```
速度比 = (当前速度 / 限速) × 100%

颜色映射:
🟢 >80% - 快速
🟢 60-80% - 中速
🟡 40-60% - 慢速
🔴 20-40% - 很慢
🔴 <20% - 停滞
```

#### 🚗 流量模式 (Flow Volume)
```
流量比 = (当前流量 / 容量) × 100%

颜色映射:
🟢 <30% - 低流量
🟢 30-60% - 中流量
🟡 60-80% - 高流量
🔴 >80% - 极高流量
```

---

### 4. 数据导出 (DataExportPanel)

**支持的数据类型**:

| 数据类型 | 格式 | 内容 |
|---------|------|------|
| 📊 Performance Metrics | CSV/JSON | 效率、速度、行程时间等 |
| 🚗 Traffic Flows | CSV/JSON | 交通流记录 (起点、终点、车辆数) |
| 🚦 Signal States | CSV/JSON | 信号灯状态历史 |
| 🗺️ Network Graph | JSON | 完整路网拓扑数据 |
| 📈 Efficiency Trend | CSV/JSON | 最近100个效率数据点 |

**CSV格式示例**:
```csv
averageEfficiency,averageSpeed,totalVehicles,completedFlows
42.5,55.3,120,45
```

**JSON格式示例**:
```json
{
  "averageEfficiency": 42.5,
  "averageSpeed": 55.3,
  "totalVehicles": 120,
  "completedFlows": 45,
  "timestamp": "2025-03-15T10:30:00"
}
```

---

## API文档

### REST API端点

**基础URL**: `http://localhost:8080/api`

#### 仿真控制

```bash
# 启动仿真
POST /simulation/start
Response: { "status": "RUNNING", "message": "Simulation started" }

# 暂停仿真
POST /simulation/pause
Response: { "status": "PAUSED" }

# 重置仿真
POST /simulation/reset
Response: { "status": "STOPPED", "message": "Simulation reset" }

# 获取仿真状态
GET /simulation/status
Response: { "state": "RUNNING", "elapsedTime": 123456 }
```

#### 数据获取

```bash
# 获取图数据
GET /simulation/graph
Response: {
  "nodes": [...],
  "edges": [...]
}

# 获取性能指标
GET /simulation/metrics
Response: {
  "averageEfficiency": 42.5,
  "averageSpeed": 55.3,
  "totalVehicles": 120,
  ...
}

# 获取效率趋势
GET /simulation/efficiency/trend?count=50
Response: [
  { "timestamp": "2025-03-15T10:30:00", "efficiency": 42.5 },
  { "timestamp": "2025-03-15T10:30:05", "efficiency": 43.1 },
  ...
]

# 获取车辆列表
GET /simulation/vehicles
Response: [
  { "id": "VEH-001", "x": 350.5, "y": 450.2, "speed": 55.0 },
  ...
]
```

#### 交通流管理

```bash
# 创建交通流
POST /simulation/flows
Content-Type: application/json
{
  "entryPoint": "B-1",
  "destination": "B-3",
  "numberOfCars": 20
}
Response: {
  "flowId": "FLOW-001",
  "status": "CREATED"
}

# 获取所有交通流
GET /simulation/flows
Response: [
  { "id": "FLOW-001", "entryPoint": "B-1", "destination": "B-3", "numberOfCars": 20 },
  ...
]
```

#### 信号灯控制

```bash
# 切换信号模式
POST /simulation/signals/mode?mode=ADAPTIVE
Response: { "mode": "ADAPTIVE", "message": "Signal mode changed" }

# 获取信号状态
GET /simulation/signals
Response: [
  { "nodeId": "I-1", "state": "GREEN", "remainingTime": 15 },
  { "nodeId": "I-2", "state": "RED", "remainingTime": 25 },
  ...
]
```

### WebSocket端点

**连接地址**: `ws://localhost:8080/ws`

**使用SockJS客户端**:

```javascript
import SockJS from 'sockjs-client';
import { Stomp } from '@stomp/stompjs';

const socket = new SockJS('http://localhost:8080/ws');
const stompClient = Stomp.over(socket);

stompClient.connect({}, () => {
  // 订阅仿真更新 (每秒推送)
  stompClient.subscribe('/topic/simulation', (message) => {
    const data = JSON.parse(message.body);
    console.log('仿真数据:', data);
  });

  // 订阅性能指标 (每5秒推送)
  stompClient.subscribe('/topic/metrics', (message) => {
    const metrics = JSON.parse(message.body);
    console.log('指标数据:', metrics);
  });
});
```

**消息格式**:

```json
// /topic/simulation
{
  "metrics": { /* 性能指标 */ },
  "vehicles": [ /* 车辆列表 */ ],
  "edges": [ /* 路段状态 */ ]
}

// /topic/metrics
{
  "averageEfficiency": 42.5,
  "averageSpeed": 55.3,
  "timestamp": "2025-03-15T10:30:00"
}
```

---

## 效率计算公式

### 数学定义

$$
E = \frac{\sum_{i=1}^{n} (N_i \times L_i / t_i)}{\sum_{i=1}^{n} N_i}
$$

其中:
- **E**: 效率值 (km/h)
- **N<sub>i</sub>**: 交通流 i 中的车辆数量
- **L<sub>i</sub>**: 交通流 i 的道路总长度 (km)
- **t<sub>i</sub>**: 交通流 i 的旅行时间 (小时)
- **n**: 交通流总数

### Java实现

```java
public double calculateEfficiency() {
    double sumWeightedSpeed = 0.0;
    int totalVehicles = 0;

    for (TrafficFlow flow : activeFlows) {
        int vehicles = flow.getNumberOfCars();
        double distance = flow.getTotalDistance(); // km
        double travelTime = flow.getTravelTime() / 3600.0; // 转换为小时

        if (travelTime > 0) {
            sumWeightedSpeed += vehicles * (distance / travelTime);
            totalVehicles += vehicles;
        }
    }

    return totalVehicles > 0 ? sumWeightedSpeed / totalVehicles : 0.0;
}
```

### 优化目标

通过调整信号灯控制策略，**最小化旅行时间 t<sub>i</sub>**，从而**最大化效率值 E**。

**优化策略**:
- **FIXED模式**: 固定时长信号灯 (30秒绿灯 / 30秒红灯)
- **ADAPTIVE模式**: 根据实时队列长度动态调整
- **INTELLIGENT模式**: 基于历史数据和预测算法的最优控制

**预期提升**:
- ADAPTIVE vs FIXED: **10-15% 效率提升**
- INTELLIGENT vs FIXED: **20-30% 效率提升**

---

## 文档导航

### 📚 完整文档集

| 文档 | 说明 | 受众 |
|------|------|------|
| [📄 README.md](README.md) | 项目概述和快速入门 (本文件) | 所有用户 |
| [📄 USER_GUIDE.md](USER_GUIDE.md) | 详细用户使用说明书 | 终端用户 |
| [📄 ROADMAP.md](ROADMAP.md) | 未来扩展规划路线图 | 开发者/管理者 |
| [📄 SETUP_GUIDE.md](SETUP_GUIDE.md) | 环境配置和故障排除 | 开发者 |
| [📄 ARLINGTON_MAP.md](ARLINGTON_MAP.md) | 阿灵顿路网数据详解 | 研究人员 |
| [📄 CLAUDE.md](CLAUDE.md) | AI助手协作指引 | AI开发者 |

### 🔗 快速链接

- **新用户?** 从 [快速开始](#快速开始) 开始
- **详细操作?** 查看 [USER_GUIDE.md](USER_GUIDE.md)
- **环境问题?** 参考 [SETUP_GUIDE.md](SETUP_GUIDE.md)
- **未来规划?** 查看 [ROADMAP.md](ROADMAP.md)
- **API调用?** 见 [API文档](#api文档)

---

## 演示截图

### 系统主界面
```
┌────────────────────────────────────────────────────────────┐
│  🚦 交通信号优化系统            [🟢 已连接]              │
├────────────────────────────────────────────────────────────┤
│                                                            │
│  [▶️ Start] [⏸️ Pause] [🔄 Reset]     状态: 🟢 RUNNING   │
│                                                            │
├────────────────────────────────────────────────────────────┤
│                                                            │
│  ┌──────────────────────────────────────────────────────┐ │
│  │ 📊 Real-time System Statistics      [🔴 LIVE]       │ │
│  ├──────────────────────────────────────────────────────┤ │
│  │ ⚡ 42.5 km/h  🏃 55.3 km/h  🚗 120  ✅ 45           │ │
│  │ 🛣️ 24.5 km   ⏱️ 5.2 min    🚦 35%   📊 110 v/min   │ │
│  └──────────────────────────────────────────────────────┘ │
│                                                            │
│  ┌──────────────────────────────────────────────────────┐ │
│  │              🗺️ 地图可视化 + 热力图                   │ │
│  │                                                       │ │
│  │    🔵 I-1 ──🟢──► 🔵 I-2 ──🟡──► 🔵 I-3           │ │
│  │     │                │                │              │ │
│  │    🟢               🟡               🔴              │ │
│  │     │                │                │              │ │
│  │    🔴 B-1          🔵 I-5          🔴 B-3           │ │
│  │                                                       │ │
│  │  [🔍 搜索] [🚗 流量] [📊 监控] [🚦 信号] [💾 导出]   │ │
│  └──────────────────────────────────────────────────────┘ │
│                                                            │
│  ┌──────────────────────────────────────────────────────┐ │
│  │         📈 效率趋势图                                 │ │
│  │  50 km/h ┤                           ╱╲              │ │
│  │  40 km/h ┤                  ╱╲     ╱  ╲             │ │
│  │  30 km/h ┤        ╱╲      ╱  ╲   ╱    ╲            │ │
│  │  20 km/h ┼────────╯──╲────╯────╲─╯──────╲────────   │ │
│  │          0s    10s   20s   30s   40s   50s          │ │
│  └──────────────────────────────────────────────────────┘ │
│                                                            │
├────────────────────────────────────────────────────────────┤
│  交通信号优化系统 - INFO6205 Final Project                │
│  作者: Chengkun Liao, Mingjie Shen                        │
└────────────────────────────────────────────────────────────┘
```

> 注: 实际界面为图形化UI，此为ASCII艺术示意图

### 核心界面组件

1. **地图视图** - SVG渲染的阿灵顿路网，20个节点，48条边
2. **统计卡片** - 8项KPI实时更新，颜色编码
3. **热力图** - 拥堵/速度/流量三种模式切换
4. **趋势图表** - 效率值时间序列可视化
5. **浮动面板** - 9个功能模块，独立交互

---

## 贡献指南

### 如何贡献

我们欢迎所有形式的贡献! 🎉

**贡献方式**:
- 🐛 报告Bug
- 💡 提出新功能建议
- 📝 改进文档
- 💻 提交代码

### 报告Bug

1. 访问 [GitHub Issues](https://github.com/your-username/INFO6205_Final_Project/issues)
2. 点击 "New Issue"
3. 选择 "Bug Report" 模板
4. 提供以下信息:
   - 操作系统和浏览器版本
   - Java版本 (`java -version`)
   - Node.js版本 (`node -v`)
   - 复现步骤
   - 期望行为 vs 实际行为
   - 错误日志或截图

### 提交代码

```bash
# 1. Fork仓库
# 2. 创建功能分支
git checkout -b feature/your-feature-name

# 3. 提交更改
git add .
git commit -m "Add: your feature description"

# 4. 推送到分支
git push origin feature/your-feature-name

# 5. 创建Pull Request
```

**代码规范**:
- Java: 遵循Google Java Style Guide
- JavaScript: 使用ESLint + Prettier
- 组件命名: PascalCase (如 `MapVisualization.jsx`)
- 函数命名: camelCase (如 `calculateEfficiency()`)
- 注释: 关键逻辑必须添加注释

### 开发路线图

查看 [ROADMAP.md](ROADMAP.md) 了解:
- Phase 1: 即时增强功能 (1-3天)
- Phase 2: 中期功能 (1-2周)
- Phase 3: 长期扩展 (未来规划)

---

## 许可证

本项目采用 **MIT License** 开源协议。

```
MIT License

Copyright (c) 2025 Chengkun Liao, Mingjie Shen

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

---

## 致谢

### 团队成员

- **Chengkun Liao** - 后端开发、算法实现
- **Mingjie Shen** - 前端开发、UI/UX设计

### 技术支持

- **Spring Boot Team** - 提供强大的后端框架
- **React Team** - 提供现代化的前端库
- **Vite Team** - 提供极速构建工具
- **Northeastern University** - 提供学习环境和资源

### 参考资料

- [Dijkstra算法](https://en.wikipedia.org/wiki/Dijkstra%27s_algorithm)
- [交通流理论](https://en.wikipedia.org/wiki/Traffic_flow)
- [Spring Boot文档](https://spring.io/projects/spring-boot)
- [React文档](https://react.dev/)

---

## 联系方式

**课程**: INFO6205 - Program Structure and Algorithms

**学期**: Spring 2025

**学校**: Northeastern University

**项目仓库**: [GitHub](https://github.com/your-username/INFO6205_Final_Project)

**反馈邮箱**: [your-email@northeastern.edu]

---

**最后更新**: 2025-03-15

**版本**: 1.0.0

**状态**: ✅ Production Ready

---

<div align="center">

**🚦 让城市交通更智能 🚦**

Made with ❤️ by Chengkun Liao & Mingjie Shen

</div>
