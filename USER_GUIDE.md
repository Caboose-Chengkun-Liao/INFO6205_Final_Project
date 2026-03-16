# 📘 交通信号优化系统 - 用户使用说明书

**Traffic Signal Optimization System User Guide**

**版本**: 1.0
**最后更新**: 2025-03-15
**作者**: Chengkun Liao, Mingjie Shen
**项目**: INFO6205 Final Project

---

## 📋 目录

1. [系统简介](#系统简介)
2. [快速开始](#快速开始)
3. [功能模块详解](#功能模块详解)
4. [常见操作流程](#常见操作流程)
5. [高级功能](#高级功能)
6. [故障排除](#故障排除)
7. [常见问题 FAQ](#常见问题-faq)
8. [术语表](#术语表)

---

## 系统简介

### 什么是交通信号优化系统？

交通信号优化系统是一个基于真实路网数据的交通仿真与优化平台。它模拟了美国弗吉尼亚州阿灵顿市的道路网络，通过可视化展示车辆运行、信号灯控制和交通流量，帮助用户：

- 🚦 **优化信号灯时序** - 减少等待时间，提高通行效率
- 📊 **分析交通性能** - 实时监控关键指标（速度、拥堵度、效率）
- 🗺️ **可视化路网状态** - 直观查看道路拥堵情况
- 📈 **导出仿真数据** - 支持 CSV/JSON 格式，便于进一步分析

### 系统架构

```
┌─────────────────┐         ┌─────────────────┐
│   前端界面      │  HTTP   │   后端服务器    │
│   (React)       │◄───────►│  (Spring Boot)  │
│  localhost:5174 │WebSocket│  localhost:8080 │
└─────────────────┘         └─────────────────┘
```

- **前端**: React 18 + Vite，提供交互式可视化界面
- **后端**: Spring Boot 3.2.0 + Java 18，处理仿真逻辑和数据管理
- **通信**: REST API + WebSocket 实时双向通信

### 核心功能

| 功能模块 | 说明 | 图标 |
|---------|------|------|
| 地图可视化 | SVG 渲染的交互式路网地图 | 🗺️ |
| 仿真控制 | 启动/暂停/重置仿真 | ▶️⏸️🔄 |
| 交通流管理 | 创建和管理车辆流 | 🚗 |
| 性能监控 | 实时 KPI 指标展示 | 📊 |
| 信号灯控制 | 切换不同优化模式 | 🚦 |
| 节点搜索 | 快速定位路口和边界点 | 🔍 |
| 统计仪表板 | 8 项关键性能指标 | 📈 |
| 数据导出 | CSV/JSON 格式导出 | 💾 |
| 热力图叠加 | 拥堵/速度/流量可视化 | 🌡️ |

---

## 快速开始

### 系统要求

**软件环境**:
- Node.js 16+ (推荐 18.x)
- Java 18 (必须，不支持 Java 25)
- Maven 3.6+
- 现代浏览器 (Chrome 90+, Firefox 88+, Safari 14+, Edge 90+)

**硬件要求**:
- CPU: 双核 2.0 GHz 或更高
- 内存: 4 GB RAM 最低 (推荐 8 GB)
- 磁盘空间: 500 MB 可用空间

### 安装步骤

#### 1. 克隆项目

```bash
git clone https://github.com/your-repo/INFO6205_Final_Project.git
cd INFO6205_Final_Project
```

#### 2. 启动后端服务

```bash
cd backend

# 确保使用 Java 18
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-18.0.2.1.jdk/Contents/Home

# 编译并运行
mvn clean install
mvn spring-boot:run -DskipTests
```

**成功标志**:
```
2025-03-15 10:30:00.123  INFO 12345 --- [main] c.t.o.TrafficOptimizationApplication : Started TrafficOptimizationApplication in 5.234 seconds
```

后端服务运行在 `http://localhost:8080`

#### 3. 启动前端应用

打开新终端窗口：

```bash
cd frontend

# 安装依赖 (首次运行)
npm install

# 启动开发服务器
npm run dev
```

**成功标志**:
```
  VITE v8.0.0  ready in 1234 ms

  ➜  Local:   http://localhost:5174/
  ➜  Network: use --host to expose
```

前端应用运行在 `http://localhost:5174`

#### 4. 访问应用

在浏览器中打开 `http://localhost:5174`，您将看到系统主界面。

**连接状态检查**:
- 右上角显示 **已连接** (绿点) 表示 WebSocket 连接成功
- 如果显示 **未连接** (灰点)，请检查后端服务是否正常运行

---

## 功能模块详解

### 1. 控制面板 (Control Panel) ▶️

**位置**: 页面左上角

**功能**:
- **启动仿真**: 点击 "Start" 按钮开始仿真
- **暂停仿真**: 点击 "Pause" 按钮暂停仿真
- **重置仿真**: 点击 "Reset" 按钮清空所有车辆和数据

**状态指示器**:
- 🟢 **RUNNING** - 仿真正在运行
- 🟡 **PAUSED** - 仿真已暂停
- 🔴 **STOPPED** - 仿真已停止

**使用示例**:
1. 点击 "Start" 启动仿真
2. 观察车辆在地图上移动
3. 点击 "Pause" 暂停观察当前状态
4. 点击 "Resume" 继续仿真
5. 点击 "Reset" 开始新的仿真

---

### 2. 地图可视化 (Map Visualization) 🗺️

**位置**: 页面中央主区域

**元素说明**:

#### 节点 (Nodes)
- **蓝色圆圈** - 路口 (Intersection)
- **红色方块** - 边界点 (Boundary Entry/Exit)
- 节点上标注 ID (如 "I-1", "B-1")

#### 边 (Edges)
- **蓝色线条** - 道路连接
- **箭头** - 表示单向通行方向
- 线条粗细表示道路容量

#### 车辆
- **绿色小圆点** - 移动中的车辆
- 车辆沿着道路边缘移动

#### 交互操作
- **缩放**: 鼠标滚轮上下滚动
- **平移**: 鼠标点击拖动地图
- **节点悬停**: 鼠标悬停在节点上显示详细信息
- **边悬停**: 鼠标悬停在边上显示道路信息

**使用技巧**:
- 放大查看局部区域的详细交通情况
- 缩小查看整个路网的宏观状态
- 点击节点可在节点搜索面板中高亮显示

---

### 3. 交通流创建面板 (Traffic Flow Panel) 🚗

**位置**: 右下角浮动按钮 (紫色，车辆图标)

**功能**: 创建从出发点到目的地的车辆流

**操作步骤**:

1. **点击浮动按钮** 打开面板

2. **选择出发点**:
   - 下拉菜单选择边界点 (如 "B-1")
   - 只能选择 BOUNDARY 类型的节点作为出发点

3. **选择目的地**:
   - 下拉菜单选择目的地 (如 "B-3")
   - 可以选择任意节点

4. **设置车辆数量**:
   - 滑块调整: 1-50 辆
   - 或直接输入数字

5. **点击 "Create Flow"**

**成功提示**:
- 绿色提示框: "✅ 交通流创建成功!"
- 车辆将开始从出发点生成

**失败原因**:
- ❌ 出发点和目的地相同
- ❌ 未找到有效路径
- ❌ 仿真未启动

**最佳实践**:
- 先启动仿真，再创建交通流
- 选择不同的出发点和目的地组合，观察路径差异
- 逐步增加车辆数量，观察拥堵形成过程

---

### 4. 性能监控面板 (Performance Monitor) 📊

**位置**: 右侧浮动按钮 (绿色，图表图标)

**两种显示模式**:

#### 紧凑视图 (Compact View)
- 显示当前网络效率值
- 趋势指示器:
  - ↗ 上升 (绿色)
  - ↘ 下降 (红色)
  - → 持平 (灰色)

#### 扩展视图 (Expanded View)
点击浮动按钮展开，显示 6 项指标:

| 指标 | 说明 | 单位 |
|------|------|------|
| Network Efficiency | 网络效率 | km/h |
| Average Speed | 平均速度 | km/h |
| Total Vehicles | 当前车辆总数 | 辆 |
| Avg Travel Time | 平均行程时间 | 秒 |
| Total Distance | 累计行驶距离 | km |
| Active Flows | 活跃交通流数量 | 个 |

**效率趋势图表**:
- X 轴: 时间
- Y 轴: 效率值 (km/h)
- 显示最近 50 个数据点
- 实时更新 (每 5 秒)

**颜色编码**:
- 🟢 绿色: 性能良好
- 🟡 黄色: 性能中等
- 🔴 红色: 性能较差

**使用场景**:
- 观察不同信号模式下的效率变化
- 对比不同流量下的性能指标
- 导出趋势数据进行离线分析

---

### 5. 信号灯控制面板 (Signal Control Panel) 🚦

**位置**: 右侧浮动按钮 (蓝色，信号灯图标)

**三种信号控制模式**:

#### 🕐 FIXED (固定时序)
- **说明**: 固定的红绿灯时长
- **绿灯时长**: 30 秒
- **红灯时长**: 30 秒
- **适用场景**: 流量均匀、可预测的道路

#### ⚡ ADAPTIVE (自适应)
- **说明**: 根据实时队列长度动态调整
- **调整策略**: 队列长的方向优先给绿灯
- **响应速度**: 快速 (每 10 秒评估一次)
- **适用场景**: 流量波动较大的路口

#### 🧠 INTELLIGENT (智能优化)
- **说明**: 基于算法的最优信号时序
- **优化目标**: 最小化总等待时间
- **考虑因素**: 历史数据 + 预测流量
- **适用场景**: 复杂路网、多路口协调

**切换模式**:
1. 点击浮动按钮打开面板
2. 点击想要的模式按钮
3. 系统自动应用新模式
4. 观察效率指标变化

**信号状态显示**:
- 每个路口的当前信号状态 (🟢 绿灯 / 🔴 红灯)
- 剩余时间倒计时

**实验建议**:
- 先使用 FIXED 模式建立基准
- 切换到 ADAPTIVE 观察改进
- 最后尝试 INTELLIGENT 模式
- 记录每种模式下的效率值进行对比

---

### 6. 节点搜索面板 (Node Search Panel) 🔍

**位置**: 左侧浮动按钮 (橙色，搜索图标)

**功能**: 快速查找和定位路网节点

**搜索功能**:
- **按 ID 搜索**: 输入 "I-1" 查找路口 1
- **按名称搜索**: 输入节点名称 (如果有)
- **实时过滤**: 输入时即时显示匹配结果

**类型筛选**:
- **All**: 显示所有节点 (20 个)
- **Intersections**: 仅显示路口 (12 个)
- **Boundaries**: 仅显示边界点 (8 个)

**节点卡片信息**:
```
🚦 I-1                    ← 图标 + ID
Intersection              ← 类型
INTERSECTION              ← 类型标签 (蓝色)
X: 350.00  Y: 450.00      ← 坐标
```

**交互操作**:
1. 点击节点卡片
2. 地图自动居中到该节点
3. 节点高亮显示
4. 在 App.jsx 中触发 `onNodeSelect` 回调

**使用场景**:
- 快速定位特定路口
- 查看节点的精确坐标
- 过滤出所有边界点作为交通流起点

---

### 7. 统计仪表板 (Statistics Dashboard) 📈

**位置**: 页面上方 (地图上方)

**8 项关键 KPI**:

#### ⚡ Network Efficiency (网络效率)
- **定义**: 系统整体运行效率
- **公式**: E = Σ(Ni × Li / ti) / Σ(Ni)
  - Ni = 车辆数
  - Li = 行驶距离
  - ti = 行驶时间
- **目标值**: ≥40 km/h (优秀)

#### 🏃 Average Speed (平均速度)
- **定义**: 所有车辆的平均行驶速度
- **单位**: km/h
- **目标值**: ≥50 km/h (良好)

#### 🚗 Active Vehicles (活跃车辆)
- **定义**: 当前在路网中的车辆总数
- **包括**: 移动中 + 排队中的车辆

#### ✅ Completed Journeys (完成旅程)
- **定义**: 成功到达目的地的车辆总数
- **用途**: 衡量系统吞吐量

#### 🛣️ Total Distance (总行驶距离)
- **定义**: 所有车辆累计行驶的距离
- **单位**: km

#### ⏱️ Avg Travel Time (平均行程时间)
- **定义**: 车辆从出发到到达的平均时间
- **单位**: 分钟
- **目标**: 越低越好

#### 🚦 Network Congestion (网络拥堵度)
- **定义**: 路网整体拥堵程度
- **计算**: 加权平均各路段的拥堵率
- **范围**: 0-100%
- **评级**:
  - <40% - 畅通 (绿色)
  - 40-70% - 中度拥堵 (黄色)
  - >70% - 严重拥堵 (红色)

#### 📊 System Throughput (系统吞吐量)
- **定义**: 单位时间内的车辆处理量
- **单位**: 辆/分钟
- **公式**: (总车辆数 × 平均速度) / 60

**实时更新**:
- WebSocket 推送: 即时更新
- 轮询更新: 每 3 秒刷新一次
- 动画效果: 数值变化平滑过渡

**性能摘要卡片**:
- **Network Health**: Excellent / Good / Fair
- **Traffic Flow**: Smooth / Congested
- **System Status**: Operational / Error

**优化目标进度条**:
- 效率目标: 当前效率 / 50 km/h
- 拥堵减少: 100% - 当前拥堵度

---

### 8. 数据导出面板 (Data Export Panel) 💾

**位置**: 右下角浮动按钮 (紫色，磁盘图标)

**5 种数据类型**:

#### 📊 Performance Metrics (性能指标)
- **内容**: 效率、速度、行程时间等
- **格式**: CSV / JSON
- **文件名**: `traffic_metrics_[timestamp].csv`

**CSV 示例**:
```csv
averageEfficiency,averageSpeed,totalVehicles,completedFlows
42.5,55.3,120,45
```

**JSON 示例**:
```json
{
  "averageEfficiency": 42.5,
  "averageSpeed": 55.3,
  "totalVehicles": 120,
  "completedFlows": 45
}
```

#### 🚗 Traffic Flows (交通流)
- **内容**: 所有创建的交通流记录
- **字段**: 出发点、目的地、车辆数、创建时间

#### 🚦 Signal States (信号灯状态)
- **内容**: 每个路口的信号灯状态历史
- **字段**: 路口 ID、信号状态、时间戳

#### 🗺️ Network Graph (路网拓扑)
- **内容**: 完整的节点和边数据
- **格式**: JSON (包含坐标、容量等)
- **用途**: 导入其他分析工具

#### 📈 Efficiency Trend (效率趋势)
- **内容**: 最近 100 个效率数据点
- **格式**: CSV / JSON
- **用途**: 绘制趋势图、时间序列分析

**导出操作**:
1. 点击浮动按钮打开面板
2. 选择数据类型
3. 点击 "CSV" 或 "JSON" 按钮
4. 文件自动下载到浏览器下载文件夹

**批量导出**:
- 点击 "📦 导出全部数据 (JSON)"
- 一次性导出所有 5 种数据
- 打包为单个 JSON 文件
- 包含导出时间戳

**文件命名规范**:
```
traffic_metrics_1710489600000.csv
traffic_flows_1710489600000.json
traffic_data_complete_1710489600000.json  ← 全部数据
```

**使用场景**:
- 将数据导入 Excel 进行进一步分析
- 保存仿真结果用于报告
- 与他人分享仿真数据
- 批量处理多次仿真结果

---

### 9. 热力图叠加层 (Traffic Heatmap Overlay) 🌡️

**位置**: 右上角浮动控制面板

**3 种可视化模式**:

#### 🚦 Congestion (拥堵模式)
- **数据源**: 路段容量利用率
- **计算公式**: (当前车辆数 / 路段容量) × 100%
- **颜色映射**:
  - 🟢 绿色 (<25%) - 畅通
  - 🟢 浅绿 (25-50%) - 轻度拥堵
  - 🟡 黄色 (50-70%) - 中度拥堵
  - 🔴 红色 (70-85%) - 重度拥堵
  - 🔴 深红 (>85%) - 严重拥堵

#### 🏃 Speed (速度模式)
- **数据源**: 路段实际速度 vs 限速
- **计算公式**: (当前速度 / 限速) × 100%
- **颜色映射**:
  - 🟢 绿色 (>80%) - 快速
  - 🟢 浅绿 (60-80%) - 中速
  - 🟡 黄色 (40-60%) - 慢速
  - 🔴 红色 (20-40%) - 很慢
  - 🔴 深红 (<20%) - 停滞

#### 🚗 Flow Volume (流量模式)
- **数据源**: 路段车辆数
- **计算公式**: (当前流量 / 容量) × 100%
- **颜色映射**:
  - 🟢 绿色 (<30%) - 低流量
  - 🟢 浅绿 (30-60%) - 中流量
  - 🟡 黄色 (60-80%) - 高流量
  - 🔴 红色 (>80%) - 极高流量

**控制功能**:
- **ON/OFF 开关**: 切换热力图显示
- **模式选择**: 点击按钮切换模式
- **显示/隐藏图例**: 点击 "📖 Show/Hide Legend"

**视觉效果**:
- 路段颜色根据数据实时变化
- 拥堵路段线条加粗 (6px)
- 平滑过渡动画 (0.5 秒)
- 脉冲动画 (3 秒周期)

**使用技巧**:
- 使用拥堵模式识别瓶颈路段
- 使用速度模式查看交通流畅度
- 使用流量模式评估路网负载均衡
- 对比不同信号模式下的热力图变化

**实时更新**:
- WebSocket 订阅: `/topic/simulation`
- 轮询更新: 每 2 秒
- 自动计算并更新颜色

---

## 常见操作流程

### 流程 1: 运行完整仿真

**目标**: 从零开始运行一次完整的交通仿真

**步骤**:

1. **启动系统**
   ```bash
   # 终端 1: 启动后端
   cd backend
   mvn spring-boot:run -DskipTests

   # 终端 2: 启动前端
   cd frontend
   npm run dev
   ```

2. **打开浏览器**
   - 访问 `http://localhost:5174`
   - 检查右上角连接状态 (应为 "已连接")

3. **启动仿真**
   - 点击控制面板的 "Start" 按钮
   - 状态变为 🟢 RUNNING

4. **创建交通流**
   - 点击紫色交通流浮动按钮
   - 选择出发点: B-1
   - 选择目的地: B-3
   - 车辆数量: 20
   - 点击 "Create Flow"

5. **观察运行**
   - 地图上出现绿色车辆小点
   - 车辆沿路径移动
   - 统计仪表板数字开始变化

6. **监控性能**
   - 点击绿色性能监控浮动按钮
   - 查看实时指标
   - 观察效率趋势图

7. **切换信号模式**
   - 点击蓝色信号灯浮动按钮
   - 切换到 ADAPTIVE 模式
   - 观察效率指标变化

8. **导出数据**
   - 点击紫色数据导出浮动按钮
   - 选择 "Performance Metrics"
   - 点击 "CSV" 下载数据

9. **结束仿真**
   - 点击 "Pause" 暂停
   - 点击 "Reset" 重置仿真

**预期结果**:
- 车辆顺利从 B-1 到达 B-3
- 效率值稳定在 30-50 km/h
- CSV 文件成功下载

---

### 流程 2: 对比不同信号模式

**目标**: 对比 FIXED、ADAPTIVE、INTELLIGENT 三种模式的性能

**步骤**:

1. **准备记录表**
   ```
   | 模式        | 平均效率 | 平均速度 | 平均行程时间 |
   |------------|---------|---------|-------------|
   | FIXED      |         |         |             |
   | ADAPTIVE   |         |         |             |
   | INTELLIGENT|         |         |             |
   ```

2. **测试 FIXED 模式**
   - Reset 仿真
   - 切换到 FIXED 模式
   - Start 仿真
   - 创建交通流: B-1 → B-3, 30 辆车
   - 等待 5 分钟
   - 记录最终指标
   - 导出数据为 `fixed_mode.csv`

3. **测试 ADAPTIVE 模式**
   - Reset 仿真
   - 切换到 ADAPTIVE 模式
   - 重复上述步骤
   - 导出数据为 `adaptive_mode.csv`

4. **测试 INTELLIGENT 模式**
   - Reset 仿真
   - 切换到 INTELLIGENT 模式
   - 重复上述步骤
   - 导出数据为 `intelligent_mode.csv`

5. **数据分析**
   - 在 Excel 中打开三个 CSV 文件
   - 创建对比图表
   - 计算改进百分比

**预期结果**:
- INTELLIGENT 模式效率最高
- ADAPTIVE 模式比 FIXED 提升 10-15%
- INTELLIGENT 比 FIXED 提升 20-30%

---

### 流程 3: 识别和分析瓶颈路段

**目标**: 找出路网中的拥堵瓶颈并分析原因

**步骤**:

1. **启用热力图**
   - 打开热力图面板
   - 确保 ON 状态
   - 选择 "Congestion" 模式

2. **创建高流量场景**
   - 创建多个交通流:
     - B-1 → B-3, 20 辆
     - B-2 → B-4, 20 辆
     - B-5 → B-7, 15 辆
   - 启动仿真

3. **观察热力图**
   - 等待 2-3 分钟
   - 识别红色/深红色路段 (>70% 拥堵)
   - 记录瓶颈路段 ID

4. **使用节点搜索**
   - 打开节点搜索面板
   - 搜索瓶颈路段的起点和终点
   - 点击节点定位

5. **分析原因**
   - 检查该路段的:
     - 容量 (capacityPerKm)
     - 当前车辆数 (currentVehicleCount)
     - 信号灯状态
     - 上下游路段情况

6. **切换到速度模式**
   - 热力图切换到 "Speed" 模式
   - 查看瓶颈路段的速度下降情况

7. **尝试优化**
   - 切换信号模式为 INTELLIGENT
   - 观察瓶颈是否缓解
   - 记录改进效果

8. **生成报告**
   - 导出效率趋势数据
   - 截图热力图
   - 编写分析结论

**示例瓶颈分析**:
```
路段: Edge-12 (I-3 → I-5)
容量: 50 辆
当前车辆: 45 辆
拥堵率: 90%
原因: 下游路口 I-5 信号灯周期过长
建议: 缩短红灯时间或增加该路段容量
```

---

### 流程 4: 导出完整报告

**目标**: 生成一份包含所有数据的仿真报告

**步骤**:

1. **运行稳定仿真**
   - 创建 3-5 个交通流
   - 运行至少 10 分钟
   - 确保指标稳定

2. **截图关键界面**
   - 地图全景 (显示所有车辆)
   - 统计仪表板
   - 热力图 (拥堵模式)
   - 性能监控面板

3. **导出所有数据**
   - 点击数据导出面板
   - 点击 "📦 导出全部数据"
   - 保存文件: `traffic_data_complete_[timestamp].json`

4. **导出单项数据**
   - Performance Metrics → CSV
   - Efficiency Trend → CSV
   - Traffic Flows → JSON
   - Network Graph → JSON

5. **整理文件夹**
   ```
   报告文件夹/
   ├── screenshots/
   │   ├── map_view.png
   │   ├── dashboard.png
   │   └── heatmap.png
   ├── data/
   │   ├── complete_data.json
   │   ├── metrics.csv
   │   ├── trend.csv
   │   ├── flows.json
   │   └── graph.json
   └── report.docx
   ```

6. **编写报告文档**
   - **第 1 部分**: 仿真配置
     - 路网: 阿灵顿 (20 节点, 48 边)
     - 信号模式: INTELLIGENT
     - 运行时长: 10 分钟
   - **第 2 部分**: 性能指标
     - 表格展示 8 项 KPI
     - 插入截图
   - **第 3 部分**: 趋势分析
     - 导入 CSV 数据到 Excel
     - 绘制效率趋势折线图
   - **第 4 部分**: 瓶颈分析
     - 插入热力图截图
     - 标注拥堵路段
   - **第 5 部分**: 优化建议
     - 基于数据提出改进方案

7. **导出 PDF**
   - 将 Word 文档导出为 PDF
   - 最终报告: `traffic_simulation_report_[date].pdf`

---

## 高级功能

### 自定义路网数据

**位置**: `backend/src/main/java/com/traffic/optimization/config/GraphConfig.java`

**修改节点**:
```java
// 添加新节点
Node newNode = new Node("I-13", NodeType.INTERSECTION, 800.0, 600.0);
newNode.setName("New Intersection");
nodes.add(newNode);
```

**修改边**:
```java
// 添加新道路
Edge newEdge = new Edge("Edge-49", nodeI13, nodeI5, 1.5, 60.0, 50.0);
edges.add(newEdge);
```

**重新编译**:
```bash
mvn clean install
mvn spring-boot:run -DskipTests
```

---

### WebSocket 消息订阅

**前端订阅示例**:
```javascript
import websocketService from './services/websocket';

// 订阅仿真更新
websocketService.subscribe('/topic/simulation', (data) => {
  console.log('仿真数据:', data);
  // data.metrics - 性能指标
  // data.vehicles - 车辆列表
  // data.edges - 路段状态
});

// 订阅性能指标
websocketService.subscribe('/topic/metrics', (data) => {
  console.log('指标数据:', data);
});
```

---

### REST API 调用

**基础 URL**: `http://localhost:8080/api`

**主要端点**:

```bash
# 获取图数据
GET /simulation/graph

# 获取当前指标
GET /simulation/metrics

# 创建交通流
POST /simulation/flows
Content-Type: application/json
{
  "entryPoint": "B-1",
  "destination": "B-3",
  "numberOfCars": 20
}

# 切换信号模式
POST /simulation/signals/mode?mode=ADAPTIVE

# 获取效率趋势
GET /simulation/efficiency/trend?count=50
```

**使用 curl 测试**:
```bash
# 获取图数据
curl http://localhost:8080/api/simulation/graph

# 创建交通流
curl -X POST http://localhost:8080/api/simulation/flows \
  -H "Content-Type: application/json" \
  -d '{"entryPoint":"B-1","destination":"B-3","numberOfCars":20}'
```

---

## 故障排除

### 问题 1: 后端无法启动

**错误信息**:
```
[ERROR] Failed to execute goal org.apache.maven.plugins:maven-compiler-plugin:3.11.0:compile
```

**原因**: Java 版本不匹配

**解决方案**:
```bash
# 检查 Java 版本
java -version

# 应该显示: openjdk version "18.0.2.1"

# 如果不是,设置 JAVA_HOME
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-18.0.2.1.jdk/Contents/Home
```

---

### 问题 2: 前端显示"未连接"

**原因**: WebSocket 连接失败

**检查步骤**:

1. **确认后端运行**
   ```bash
   curl http://localhost:8080/api/simulation/graph
   # 应返回 JSON 数据
   ```

2. **检查浏览器控制台**
   - 打开 DevTools (F12)
   - 查看 Network 标签
   - 查找 WebSocket 连接 (ws://localhost:8080/ws)

3. **检查 CORS 配置**
   - 文件: `backend/src/main/java/com/traffic/optimization/config/WebSocketConfig.java`
   - 确认包含前端地址:
     ```java
     .setAllowedOrigins("http://localhost:5174")
     ```

4. **重启服务**
   ```bash
   # 停止后端 (Ctrl+C)
   # 重新启动
   mvn spring-boot:run -DskipTests
   ```

---

### 问题 3: 车辆不移动

**可能原因**:

1. **仿真未启动**
   - 检查控制面板状态
   - 应为 🟢 RUNNING

2. **未创建交通流**
   - 点击交通流面板
   - 创建至少一个流

3. **路径不存在**
   - 检查出发点和目的地
   - 确保存在有效路径

**调试步骤**:
```bash
# 查看后端日志
# 应看到类似信息:
# "Created traffic flow: B-1 -> B-3 with 20 vehicles"
# "Vehicle VEH-001 spawned at B-1"
# "Vehicle VEH-001 moving on Edge-5"
```

---

### 问题 4: 数据导出失败

**错误提示**: "❌ 导出失败: Network Error"

**解决方案**:

1. **检查后端 API**
   ```bash
   curl http://localhost:8080/api/simulation/metrics
   ```

2. **查看浏览器控制台**
   - 是否有 CORS 错误?
   - 是否有 404 错误?

3. **清除浏览器缓存**
   - 刷新页面 (Ctrl+Shift+R)

4. **检查文件权限**
   - 确保浏览器可以下载文件到默认文件夹

---

### 问题 5: 热力图不显示

**症状**: 地图上没有颜色变化

**检查清单**:

- [ ] 热力图开关是否为 ON
- [ ] 是否有车辆在路网中运行
- [ ] 浏览器控制台是否有 JavaScript 错误
- [ ] graphData 是否正确加载

**调试代码**:
```javascript
// 在浏览器控制台执行
console.log(window.graphData); // 应显示节点和边数据
```

---

## 常见问题 FAQ

### Q1: 系统支持多少辆车同时运行?

**A**: 当前版本支持最多 500 辆车。超过此数量可能导致:
- 前端渲染卡顿
- WebSocket 消息延迟
- 后端内存占用增加

**优化建议**:
- 使用 WebGL 渲染替代 SVG (Phase 4)
- 实施车辆池 (限制最大并发数)

---

### Q2: 如何修改信号灯时长?

**A**: 修改文件 `backend/src/main/java/com/traffic/optimization/model/TrafficLight.java`

```java
public class TrafficLight {
    private static final int GREEN_DURATION = 30; // 修改这里 (秒)
    private static final int RED_DURATION = 30;   // 修改这里 (秒)
}
```

重新编译后生效。

---

### Q3: 效率值为什么是负数?

**A**: 效率值不应为负数。如果出现负数,可能原因:

1. **车辆行程时间为 0**
   - 车辆刚生成,还未移动
   - 分母为 0 导致计算错误

2. **数据异常**
   - 检查后端日志
   - 查看是否有异常堆栈

**解决方案**:
```java
// 添加防护代码
public double calculateEfficiency() {
    if (travelTime <= 0) return 0;
    return (distance / travelTime) * 3600; // 转换为 km/h
}
```

---

### Q4: 可以添加更多城市吗?

**A**: 可以。参考 ROADMAP.md 中的 "Phase 3.1 多城市支持"。

**简化版步骤**:
1. 创建新的 JSON 文件: `frontend/src/data/boston_ma.json`
2. 定义节点和边
3. 在城市选择器中添加选项
4. 加载新城市数据

---

### Q5: 如何导出视频/GIF?

**A**: 当前版本不支持内置录制。推荐使用:

1. **录屏软件**:
   - Mac: QuickTime Player
   - Windows: Xbox Game Bar (Win+G)
   - 跨平台: OBS Studio

2. **浏览器插件**:
   - Loom (Chrome 插件)
   - Screen Recorder (Firefox 插件)

3. **转换为 GIF**:
   - 使用 ffmpeg:
     ```bash
     ffmpeg -i simulation.mp4 -vf "fps=10,scale=800:-1" -loop 0 simulation.gif
     ```

---

### Q6: 系统是否支持实时道路数据?

**A**: 当前版本使用模拟数据。未来版本计划支持:

- **Phase 3.4**: 云部署,集成第三方 API
- **可能的数据源**:
  - Google Maps Traffic API
  - OpenStreetMap 实时数据
  - 城市交通部门开放数据

---

### Q7: 如何贡献代码或报告 Bug?

**A**: 欢迎贡献!

**报告 Bug**:
1. 访问 GitHub Issues
2. 点击 "New Issue"
3. 提供以下信息:
   - 操作系统和浏览器版本
   - 复现步骤
   - 期望行为 vs 实际行为
   - 截图或日志

**贡献代码**:
1. Fork 仓库
2. 创建分支: `git checkout -b feature/your-feature`
3. 提交代码: `git commit -m "Add some feature"`
4. 推送分支: `git push origin feature/your-feature`
5. 创建 Pull Request

---

## 术语表

| 术语 | 英文 | 说明 |
|------|------|------|
| 节点 | Node | 路网中的点,包括路口和边界点 |
| 边 | Edge | 连接两个节点的道路 |
| 交通流 | Traffic Flow | 从出发点到目的地的一组车辆 |
| 效率 | Efficiency | 网络整体运行效率 (km/h) |
| 拥堵度 | Congestion Level | 路段容量利用率 (%) |
| 吞吐量 | Throughput | 单位时间通过的车辆数 |
| 信号时序 | Signal Timing | 红绿灯的时长配置 |
| 自适应控制 | Adaptive Control | 根据实时流量调整信号 |
| 热力图 | Heatmap | 用颜色表示数值的可视化方式 |
| WebSocket | WebSocket | 双向实时通信协议 |
| KPI | Key Performance Indicator | 关键绩效指标 |

---

## 附录

### A. 键盘快捷键 (计划中)

| 快捷键 | 功能 |
|--------|------|
| Space | 播放/暂停仿真 |
| R | 重置仿真 |
| H | 切换热力图 |
| F | 聚焦交通流表单 |
| E | 打开导出面板 |
| ? | 显示帮助 |

### B. 颜色编码规范

**状态颜色**:
- 🟢 绿色 (#10B981): 良好/成功
- 🟡 黄色 (#F59E0B): 警告/中等
- 🔴 红色 (#EF4444): 错误/差
- 🔵 蓝色 (#3B82F6): 信息/中性
- 🟣 紫色 (#8B5CF6): 特殊/高级

**节点类型**:
- 🔵 蓝色圆圈: 路口 (Intersection)
- 🔴 红色方块: 边界点 (Boundary)

### C. 数据格式规范

**CSV 导出格式**:
- 编码: UTF-8
- 分隔符: 逗号 (,)
- 换行符: LF (\n)
- 引号转义: 双引号 ("")

**JSON 导出格式**:
- 缩进: 2 空格
- 编码: UTF-8
- 日期格式: ISO 8601 (YYYY-MM-DDTHH:mm:ss)

---

## 联系方式

**作者**:
- Chengkun Liao
- Mingjie Shen

**项目仓库**: [GitHub链接]

**反馈邮箱**: [邮箱地址]

**课程**: INFO6205 - Program Structure and Algorithms
**学期**: Spring 2025
**学校**: Northeastern University

---

**文档版本**: 1.0
**发布日期**: 2025-03-15
**下次更新**: TBD

---

**感谢使用交通信号优化系统!**

如有任何问题或建议,欢迎联系我们。
