# 阿灵顿地区道路网络地图说明

## 地图概览

本系统使用的是**美国弗吉尼亚州阿灵顿郡（Arlington County, Virginia）**的真实道路网络简化模型。

阿灵顿郡位于华盛顿特区（Washington D.C.）的西侧，通过多座桥梁与首都相连，是华盛顿都会区的重要组成部分。

---

## 包含的主要区域

### 1. **Clarendon** (克拉伦登)
- **位置**: 阿灵顿的商业中心区
- **节点**: `1` - Clarendon Blvd & Wilson Blvd
- **特点**: 高密度商业区，地铁橙线站点，餐饮和购物中心
- **坐标**: (4.0, 6.0)

### 2. **Courthouse** (法院区)
- **位置**: 政府办公区
- **节点**: `2` - Courthouse Rd & Wilson Blvd
- **特点**: 县政府大楼、法院、地铁橙线站点
- **坐标**: (5.5, 6.5)

### 3. **Ballston** (鲍尔斯顿)
- **位置**: 阿灵顿西北部商业区
- **节点**: `3` - Fairfax Dr & Wilson Blvd
- **特点**: 购物中心、地铁橙线和银线站点
- **坐标**: (2.5, 6.5)

### 4. **Rosslyn** (罗斯林)
- **位置**: 紧邻华盛顿特区，Key Bridge东侧
- **节点**: `4` - Fort Myer Dr & Wilson Blvd
- **特点**: 高层建筑群、地铁蓝/橙/银线站点、通往DC的主要入口
- **坐标**: (6.5, 6.0)

### 5. **Pentagon City** (五角大楼城)
- **位置**: 五角大楼附近的商业区
- **节点**: `5` - Army Navy Dr & S Hayes St
- **特点**: Fashion Centre购物中心、地铁蓝/黄线站点、里根国家机场附近
- **坐标**: (6.0, 2.5)

### 6. **Crystal City** (水晶城)
- **位置**: 五角大楼与里根机场之间
- **节点**: `6` - Crystal Dr & 15th St
- **特点**: 亚马逊HQ2所在地、地铁蓝/黄线站点、高层公寓和办公楼
- **坐标**: (7.0, 3.0)

### 7. **Columbia Pike**
- **位置**: 阿灵顿南部主干道
- **节点**: `7`, `8` - Columbia Pike沿线路口
- **特点**: 多元文化社区、公交快速线
- **坐标**: (3.0, 2.0) - (4.5, 2.5)

---

## 主要道路

### 东西向主干道

#### **Wilson Boulevard** (威尔逊大道)
- 连接：Ballston → Clarendon → Courthouse → Rosslyn
- 距离：约4.5公里
- 特点：阿灵顿的主要商业走廊，地铁橙线/银线平行

#### **Lee Highway** (李公路)
- 连接：西部边界 → Ballston → Courthouse
- 距离：约3公里
- 特点：历史悠久的主要道路，Route 29

#### **Arlington Boulevard / Route 50** (阿灵顿大道)
- 连接：西部边界 → Rosslyn
- 距离：约4公里
- 特点：主要东西向高速公路

#### **Columbia Pike**
- 连接：南部各社区
- 距离：约3.3公里
- 特点：多元文化走廊，有专用公交车道

### 南北向连接道路
- Clarendon ↔ Route 50: 1.8公里
- Rosslyn ↔ Crystal City: 3.5公里（主要南北轴）
- Crystal City ↔ Pentagon City: 0.8公里

---

## 边界节点（交通流起终点）

### 北部边界
- **N1**: Lee Highway North Entry (通往Falls Church方向)
- **N2**: Washington Blvd North Entry (通往McLean方向)

### 南部边界
- **S1**: Columbia Pike South Entry (通往Fairfax County方向)
- **S2**: Arlington Blvd South Entry (通往Alexandria方向)

### 东部边界（通往华盛顿DC）
- **E1**: Key Bridge Entry (连接Georgetown)
- **E2**: Memorial Bridge Entry (连接林肯纪念堂)

### 西部边界
- **W1**: Route 50 West Entry (通往Fairfax方向)
- **W2**: Lee Hwy West Entry (通往Vienna方向)

---

## 网络统计

- **总节点数**: 20个
  - 路口节点（有信号灯）: 12个
  - 边界节点（交通流起终点）: 8个

- **总道路数**: 48条（双向边）
  - 主干道: 24条
  - 连接道路: 12条
  - 边界连接: 12条

- **覆盖范围**: 约8km × 8km区域

---

## 典型交通流场景

### 早高峰（通勤进城）
**主要流向**: 西部/南部边界 → Rosslyn/Crystal City (进DC)

示例流:
```
W1 (Route 50 West) → Route 50 → Rosslyn → E1 (Key Bridge)
S1 (Columbia Pike South) → Columbia Pike → Pentagon City → Crystal City → E2 (Memorial Bridge)
```

### 晚高峰（通勤出城）
**主要流向**: Rosslyn/Crystal City → 西部/南部边界

示例流:
```
E1 (Key Bridge) → Rosslyn → Wilson Blvd → Ballston → W2 (Lee Hwy West)
E2 (Memorial Bridge) → Crystal City → Pentagon City → S2 (Arlington Blvd South)
```

### 区域内交通
**主要流向**: 各商业区之间

示例流:
```
Ballston → Wilson Blvd → Clarendon → Courthouse
Pentagon City → Crystal City → Rosslyn
```

---

## 实际使用建议

### 创建测试流

使用API创建交通流来模拟真实场景：

```bash
# 早高峰：从西部进城
curl -X POST http://localhost:8080/api/simulation/flows \
  -H "Content-Type: application/json" \
  -d '{
    "entryPoint": "W1",
    "destination": "E1",
    "numberOfCars": 150
  }'

# 晚高峰：从DC出城
curl -X POST http://localhost:8080/api/simulation/flows \
  -H "Content-Type: application/json" \
  -d '{
    "entryPoint": "E1",
    "destination": "W2",
    "numberOfCars": 180
  }'

# 区域内交通
curl -X POST http://localhost:8080/api/simulation/flows \
  -H "Content-Type: application/json" \
  -d '{
    "entryPoint": "3",
    "destination": "5",
    "numberOfCars": 80
  }'
```

### 重点观测路口

以下路口通常是交通瓶颈：

1. **Rosslyn** (`4`) - 进出DC的主要通道
2. **Clarendon** (`1`) - 商业中心，多条道路交汇
3. **Pentagon City** (`5`) - 连接机场和DC
4. **Route 50 & Courthouse Rd** (`9`) - 主干道交叉

---

## 地图参考

- **真实地图**: [Google Maps - Arlington, VA](https://goo.gl/maps/arlington-va)
- **地铁图**: [WMATA Metro Map](https://www.wmata.com/rider-guide/stations/)
- **实时交通**: [Arlington County Traffic](https://traffic.arlingtonva.us/)

---

## 注意事项

1. **简化处理**: 本模型是实际路网的简化版本，仅包含主要道路和路口
2. **距离近似**: 道路距离基于实际距离的近似值
3. **信号灯**: 所有路口节点都配置了基本的红绿灯控制
4. **容量假设**: 所有道路使用相同的容量参数（50车/km）和速度限制（60km/h）

---

## 未来扩展

可以考虑添加以下真实特征：

1. **地铁站**: 在Ballston, Clarendon, Courthouse, Rosslyn等站点增加公交节点
2. **里根机场**: 添加机场交通流
3. **五角大楼**: 添加五角大楼入口道路
4. **GW Parkway**: 添加沿波托马克河的景观大道
5. **I-395**: 添加州际公路395

---

**最后更新**: 2026-03-12
**作者**: Chengkun Liao, Mingjie Shen
