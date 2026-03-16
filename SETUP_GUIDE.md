# 项目设置和使用指南

## 当前项目状态

本项目已完成以下核心功能的代码实现：

### ✅ 已完成的后端功能

1. **数据模型** (`backend/src/main/java/com/traffic/optimization/model/`)
   - `Node.java` - 路口/节点模型
   - `Edge.java` - 道路/边模型
   - `Graph.java` - 道路网络图
   - `TrafficFlow.java` - 交通流模型
   - `TrafficLight.java` - 信号灯模型
   - `NodeType.java` - 节点类型枚举

2. **算法实现** (`backend/src/main/java/com/traffic/optimization/algorithm/`)
   - `DijkstraAlgorithm.java` - 最短路径算法

3. **业务逻辑服务** (`backend/src/main/java/com/traffic/optimization/service/`)
   - `SimulationEngine.java` - 仿真引擎
   - `FlowManager.java` - 交通流管理器
   - `SignalController.java` - 信号灯控制器
   - `EfficiencyCalculator.java` - 效率计算器

4. **REST API控制器** (`backend/src/main/java/com/traffic/optimization/controller/`)
   - `SimulationController.java` - 仿真控制API

5. **WebSocket配置** (`backend/src/main/java/com/traffic/optimization/`)
   - `WebSocketConfig.java` - WebSocket配置
   - `SimulationWebSocketHandler.java` - 实时数据推送

6. **测试** (`backend/src/test/java/`)
   - `DijkstraAlgorithmTest.java` - Dijkstra算法单元测试

### ✅ 已完成的前端功能

1. **React组件** (`frontend/src/components/`)
   - `ControlPanel.jsx` - 仿真控制面板
   - `MetricsDisplay.jsx` - 性能指标展示
   - `MetricsChart.jsx` - 效率趋势图表

2. **服务层** (`frontend/src/services/`)
   - `api.js` - REST API客户端
   - `websocket.js` - WebSocket客户端

3. **主应用**
   - `App.jsx` - 主应用组件
   - `App.css` - 样式文件

---

## 已知问题

### Lombok编译问题

当前后端项目使用了Lombok注解（`@Data`, `@Getter`等）来简化代码。如果遇到编译错误显示找不到getter/setter方法，请执行以下操作：

#### 方案1: 配置IDE的Lombok支持

**IntelliJ IDEA:**
1. 安装Lombok插件: `Preferences` -> `Plugins` -> 搜索 "Lombok" -> 安装
2. 启用注解处理: `Preferences` -> `Build, Execution, Deployment` -> `Compiler` -> `Annotation Processors` -> 勾选 "Enable annotation processing"
3. 重新导入Maven项目

**Eclipse:**
1. 下载 lombok.jar
2. 运行 `java -jar lombok.jar`
3. 选择Eclipse安装目录
4. 重启Eclipse

#### 方案2: 移除Lombok依赖（推荐用于快速测试）

如果只是想快速运行项目，可以手动为每个使用`@Data`的类添加getter/setter方法。

---

## 启动步骤

### 1. 后端启动

```bash
cd backend

# 跳过测试编译并运行
mvn spring-boot:run -DskipTests

# 或者先编译再运行
mvn clean install -DskipTests
mvn spring-boot:run
```

后端将在 `http://localhost:8080` 启动

### 2. 前端启动

```bash
cd frontend

# 安装依赖（首次运行）
npm install

# 启动开发服务器
npm run dev
```

前端将在 `http://localhost:5173` 启动

---

## 使用说明

### 1. 初始化仿真

1. 打开浏览器访问 `http://localhost:5173`
2. 点击"初始化"按钮，系统会创建默认的道路网络
3. 等待初始化完成

### 2. 创建交通流

使用API或前端界面创建交通流：

```bash
curl -X POST http://localhost:8080/api/simulation/flows \
  -H "Content-Type: application/json" \
  -d '{
    "entryPoint": "A",
    "destination": "D",
    "numberOfCars": 100
  }'
```

### 3. 启动仿真

点击"启动"按钮，仿真开始运行。系统会：
- 每秒更新交通流位置
- 每秒更新信号灯状态
- 每5秒计算性能指标
- 实时推送数据到前端

### 4. 查看实时数据

前端会实时显示：
- 仿真状态和时间
- 效率值、吞吐量、平均速度等性能指标
- 效率随时间变化的趋势图

---

## API使用示例

### 获取仿真状态

```bash
curl http://localhost:8080/api/simulation/status
```

### 获取性能指标

```bash
curl http://localhost:8080/api/simulation/metrics
```

### 获取效率趋势

```bash
curl http://localhost:8080/api/simulation/efficiency/trend?count=50
```

### 设置信号优化模式

```bash
curl -X POST "http://localhost:8080/api/simulation/signals/mode?mode=TRAFFIC_ADAPTIVE"
```

可用模式:
- `FIXED_TIME` - 固定时长
- `TRAFFIC_ADAPTIVE` - 交通自适应
- `LEARNING_BASED` - 基于学习

---

## 下一步开发建议

1. **实现默认地图数据**
   - 在`SimulationController.createDefaultGraph()`中添加示例道路网络
   - 或从配置文件/数据库加载真实地图数据

2. **增强前端可视化**
   - 添加交互式地图显示道路网络
   - 实时显示车辆位置和信号灯状态
   - 支持手动调整信号灯参数

3. **完善测试**
   - 添加更多单元测试
   - 集成测试
   - 端到端测试

4. **性能优化**
   - 大规模交通网络压力测试
   - 优化算法性能
   - 数据库持久化

---

## 故障排除

### 问题: 前端无法连接WebSocket

**解决方案:**
- 确保后端已启动
- 检查CORS配置
- 查看浏览器控制台错误信息

### 问题: Maven编译失败

**解决方案:**
- 确保JDK版本为17或更高
- 清理Maven缓存: `mvn clean`
- 删除`~/.m2/repository`中的缓存并重新下载依赖

### 问题: 前端npm install失败

**解决方案:**
- 确保Node.js版本为16或更高
- 清理npm缓存: `npm cache clean --force`
- 删除`node_modules`和`package-lock.json`后重新安装

---

## 联系方式

如有问题，请联系项目作者：
- Chengkun Liao
- Mingjie Shen
