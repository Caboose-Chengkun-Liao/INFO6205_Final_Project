# Traffic Signal Optimization System - Extension Roadmap

## Project Overview

The Traffic Signal Optimization System is an enterprise-grade traffic simulation and optimization platform built on Spring Boot and React. This document details the planned extension directions and implementation roadmap for the system.

---

## Table of Contents

1. [Current System State](#current-system-state)
2. [Phase 1: Immediate Enhancements (1-3 days)](#phase-1-immediate-enhancements-1-3-days)
3. [Phase 2: Mid-term Features (1-2 weeks)](#phase-2-mid-term-features-1-2-weeks)
4. [Phase 3: Long-term Extensions](#phase-3-long-term-extensions)
5. [Phase 4: Performance Optimization](#phase-4-performance-optimization)
6. [Phase 5: User Experience Enhancements](#phase-5-user-experience-enhancements)
7. [Phase 6: Advanced Features](#phase-6-advanced-features)
8. [Technology Architecture Evolution](#technology-architecture-evolution)
9. [Priority Matrix](#priority-matrix)
10. [Implementation Timeline](#implementation-timeline)
11. [Success Metrics](#success-metrics)

---

## Current System State

### Completed Core Modules

**Backend (Spring Boot 3.2.0 + Java 18)**
- Weighted directed graph structure (20 nodes, 48 edges — Arlington road network)
- Dijkstra shortest-path algorithm
- Traffic flow simulation engine (discrete event simulation)
- **Dynamic speed adjustment system** — congestion slowdown based on road occupancy rate
  - Occupancy > 90%: speed reduced to 30% (severe congestion)
  - Occupancy > 75%: speed reduced to 50% (heavy congestion)
  - Occupancy > 50%: speed reduced to 75% (moderate congestion)
  - Occupancy > 25%: speed reduced to 90% (light congestion)
- Efficiency formula: E = Sigma(Ni x Li / ti) / Sigma(Ni)
- WebSocket real-time data push (SockJS + STOMP)
- REST API (CORS configured)
- JSON serialization optimization (@JsonIdentityInfo to prevent circular references)

**Frontend (React 18 + Vite)**
1. **MapVisualization.jsx** — SVG map visualization (1600x800px)
   - Desktop-optimized, full-width layout
   - Real-time vehicle animation
   - Node / edge interaction

2. **ControlPanel.jsx** — Simulation control panel
   - Start / pause / reset
   - Status indicator

3. **TrafficFlowPanel.jsx** — Traffic flow creation panel
   - Origin / destination selection
   - Vehicle count configuration
   - Form validation

4. **PerformanceMonitor.jsx** — Performance monitoring panel
   - Compact view + expanded view
   - 6 key metrics
   - Efficiency trend chart

5. **SignalControlPanel.jsx** — Signal light control panel
   - 3 modes: FIXED / ADAPTIVE / GREEN_WAVE
   - Real-time signal state display

6. **NodeSearchPanel.jsx** — Node search panel
   - Real-time search filtering
   - Type filtering (intersection / boundary)
   - Click-to-locate

7. **StatisticsDashboard.jsx** — Statistics dashboard
   - 8 KPI metric cards
   - Real-time data updates
   - Performance summary

8. **DataExportPanel.jsx** — Data export panel
   - 5 data type exports
   - CSV / JSON format support
   - Batch export

9. **TrafficHeatmapOverlay.jsx** — Traffic heatmap
   - 3 visualization modes (congestion / speed / flow)
   - 5-level color gradient
   - Toggle control + legend

### Current Performance Metrics

- **Frontend load time**: ~2 seconds
- **API response time**: ~100ms
- **WebSocket latency**: < 50ms
- **Max supported vehicles**: 500 (unoptimized)
- **Frame rate**: 30-60 FPS (SVG rendering)

---

## Phase 1: Immediate Enhancements (1-3 days)

### 1.1 Real-time Notification and Alert System

**Component**: `NotificationCenter.jsx`

**Features**:
- Toast notifications (top-right corner)
- 4 notification levels:
  - Info: traffic flow created successfully
  - Warning: segment congested (> 70%)
  - Critical: system error
  - Success: operation succeeded
- Notification history (last 50 entries)
- Sound toggle (optional)
- Auto-dismiss (3 / 5 / 10 seconds, configurable)

**Technical implementation**:
```javascript
// NotificationCenter.jsx
import { useState, useEffect } from 'react';
import websocketService from '../services/websocket';

const NotificationCenter = () => {
  const [notifications, setNotifications] = useState([]);

  useEffect(() => {
    websocketService.subscribe('/topic/notifications', (notification) => {
      addNotification(notification);
    });
  }, []);

  const addNotification = (notification) => {
    const id = Date.now();
    setNotifications(prev => [...prev, { id, ...notification }]);
    setTimeout(() => removeNotification(id), notification.duration || 5000);
  };

  // Render toast stack
};
```

**Backend support**:
```java
// NotificationService.java
@Service
public class NotificationService {
    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    public void sendNotification(String message, NotificationLevel level) {
        Notification notification = new Notification(message, level, LocalDateTime.now());
        messagingTemplate.convertAndSend("/topic/notifications", notification);
    }
}
```

**Effort**: 1 day
**Priority**: P0 (highest)
**Dependencies**: none

---

### 1.2 System Event Log Viewer

**Component**: `EventLogPanel.jsx`

**Features**:
- Time-series event log
- Event types:
  - `SIMULATION_STARTED`
  - `SIMULATION_STOPPED`
  - `FLOW_CREATED`
  - `VEHICLE_SPAWNED`
  - `VEHICLE_ARRIVED`
  - `SIGNAL_MODE_CHANGED`
  - `CONGESTION_DETECTED`
  - `ERROR_OCCURRED`
- Filters: by type, time range, keyword search
- Export log as JSON / TXT

**Data structure**:
```java
@Data
public class SystemEvent {
    private String id;
    private LocalDateTime timestamp;
    private EventType type;
    private String message;
    private String source;
    private Map<String, Object> metadata;
}
```

**Frontend implementation**:
```javascript
const EventLogPanel = () => {
  const [events, setEvents] = useState([]);
  const [filterType, setFilterType] = useState('ALL');

  useEffect(() => {
    const interval = setInterval(async () => {
      const response = await api.get('/simulation/events?limit=100');
      setEvents(response.data);
    }, 2000);
    return () => clearInterval(interval);
  }, []);

  // Render filterable event list
};
```

**Effort**: 1 day
**Priority**: P0
**Dependencies**: backend event recording service

---

### 1.3 Historical Data Comparison

**Component**: `HistoricalComparisonPanel.jsx`

**Features**:
- Compare current simulation with historical run results
- Display improvement percentages
- Side-by-side comparison table
- Overlaid time-series charts

**API endpoint**:
```
GET /simulation/history?limit=10
Response:
[
  {
    "id": "sim-20250315-001",
    "timestamp": "2025-03-15T10:30:00",
    "avgEfficiency": 42.5,
    "avgSpeed": 55.2,
    "totalVehicles": 150
  },
  ...
]
```

**Effort**: 1.5 days
**Priority**: P1
**Dependencies**: database integration

---

### 1.4 Advanced Map Controls

**Enhanced component**: `MapVisualization.jsx`

**New features**:
1. **Mini-map navigator** (bottom-right corner)
   - Thumbnail view showing the current viewport
   - Click mini-map to jump quickly

2. **Zoom level indicator**
   - Shows current zoom: 100% / 150% / 200%
   - Preset zoom buttons

3. **Pan reset button**
   - One-click return to center

4. **Screenshot**
   - Using html2canvas
   - Export current map view as PNG

5. **Grid overlay toggle**
   - Show coordinate grid (every 100 units)

**Technical implementation**:
```javascript
import html2canvas from 'html2canvas';

const captureScreenshot = () => {
  html2canvas(mapRef.current).then(canvas => {
    const link = document.createElement('a');
    link.download = `map_screenshot_${Date.now()}.png`;
    link.href = canvas.toDataURL();
    link.click();
  });
};
```

**Effort**: 2 days
**Priority**: P1
**Dependencies**: install html2canvas library

---

## Phase 2: Mid-term Features (1-2 weeks)

### 2.1 Route Replay and Time Machine

**Component**: `RouteReplayPanel.jsx`

**Core features**:
- **Playback controls**:
  - Play / pause / stop
  - Speed control: 0.5x, 1x, 2x, 5x, 10x
  - Progress bar scrubbing (timeline)

- **Timeline visualization**:
  - Show each vehicle's life cycle
  - Key event markers (departure, arrival, red lights)

- **Hotspot identification**:
  - Automatically mark frequently congested segments
  - Show average wait times

**Backend data structure**:
```java
@Data
public class VehicleSnapshot {
    private String vehicleId;
    private LocalDateTime timestamp;
    private String currentEdgeId;
    private double position; // 0.0 to 1.0 along edge
    private double speed;
}

// Snapshot stored every 5 seconds
```

**Frontend implementation**:
```javascript
const RouteReplayPanel = () => {
  const [playbackTime, setPlaybackTime] = useState(0);
  const [playbackSpeed, setPlaybackSpeed] = useState(1);
  const [isPlaying, setIsPlaying] = useState(false);

  useEffect(() => {
    if (!isPlaying) return;
    const interval = setInterval(() => {
      setPlaybackTime(prev => prev + (1000 * playbackSpeed));
    }, 1000);
    return () => clearInterval(interval);
  }, [isPlaying, playbackSpeed]);

  // Fetch snapshot at playbackTime and render
};
```

**Effort**: 5 days
**Priority**: P1
**Dependencies**: backend snapshot storage service

---

### 2.2 Advanced Analytics Dashboard

**Component**: `AdvancedAnalytics.jsx`

**Feature modules**:

#### 2.2.1 Traffic Flow Analysis
- **Origin-Destination (O-D) matrix heatmap**:
  ```
       B1    B2    B3    B4
  B1   -     120   80    50
  B2   100   -     150   90
  B3   70    130   -     110
  B4   60    85    95    -
  ```

- **Peak hour identification**:
  - Automatically detect traffic peaks (> 1.5x average)
  - Show duration

- **Route popularity ranking**:
  - Top 10 most-used routes
  - Show usage frequency

#### 2.2.2 Signal Light Performance Analysis
- **Green-time utilization**:
  ```
  Utilization = (actual vehicles passing / max throughput during green) x 100%
  ```

- **Queue formation patterns**:
  - Average queue length curve
  - Maximum queue length

- **Red-light arrival statistics** (simulated):
  - Count vehicles arriving during red

#### 2.2.3 Road Network Analysis
- **Bottleneck identification**:
  - Mark segments with capacity utilization > 80%
  - Calculate bottleneck impact radius

- **Critical path analysis**:
  - Find paths carrying the most traffic
  - Calculate path redundancy

- **Load balance metric**:
  ```
  Load Balance Score = 1 - (sigma / mu)
  where sigma = segment flow standard deviation, mu = average flow
  ```

**Visualization**:
- ECharts charting library
- Interactive charts (zoom, filter)
- Real-time updates

**Effort**: 7 days
**Priority**: P1
**Dependencies**: ECharts library, backend statistics API

---

### 2.3 Multi-scenario Comparison

**Component**: `ScenarioComparisonPanel.jsx`

**Features**:
- Run up to 4 simulation scenarios simultaneously
- Scenario configuration templates:
  - Morning peak (7:00-9:00, high volume, ADAPTIVE mode)
  - Evening peak (17:00-19:00, high volume, GREEN_WAVE mode)
  - Weekend (low volume, FIXED mode)
  - Incident scenario (random segment closure)

- Side-by-side comparison view:
  - 4 map views (2x2 grid)
  - Unified timeline
  - Metrics comparison table

**Backend support**:
```java
@Service
public class MultiScenarioService {
    private Map<String, SimulationEngine> scenarios = new ConcurrentHashMap<>();

    public void startScenario(String scenarioId, ScenarioConfig config) {
        SimulationEngine engine = new SimulationEngine(config);
        scenarios.put(scenarioId, engine);
        engine.start();
    }

    public Map<String, Metrics> getAllMetrics() {
        return scenarios.entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                e -> e.getValue().getMetrics()
            ));
    }
}
```

**Effort**: 5 days
**Priority**: P2
**Dependencies**: backend multi-instance support

---

### 2.4 Custom Report Generator

**Component**: `ReportGenerator.jsx`

**Report structure**:
```
1. Executive Summary
   - Key findings
   - Improvement recommendations

2. Simulation Configuration
   - Run time
   - Scenario parameters

3. Performance Metrics
   - Tables + charts

4. Detailed Analysis
   - Bottleneck segments
   - Signal light efficiency

5. Recommendations
   - AI-generated suggestions

6. Appendix
   - Raw data tables
```

**Export formats**:
- **PDF**: using jsPDF + jsPDF-AutoTable
- **Word**: using docx library
- **HTML**: standalone HTML file

**Example code**:
```javascript
import jsPDF from 'jspdf';
import 'jspdf-autotable';

const generatePDF = (reportData) => {
  const doc = new jsPDF();

  // Title
  doc.setFontSize(20);
  doc.text('Traffic Optimization Report', 20, 20);

  // Metrics table
  doc.autoTable({
    head: [['Metric', 'Value', 'Target']],
    body: reportData.metrics.map(m => [m.name, m.value, m.target]),
    startY: 30
  });

  // Chart image
  const chartCanvas = document.getElementById('efficiencyChart');
  const chartImage = chartCanvas.toDataURL('image/png');
  doc.addImage(chartImage, 'PNG', 20, doc.lastAutoTable.finalY + 10, 170, 100);

  doc.save('traffic_report.pdf');
};
```

**Effort**: 4 days
**Priority**: P2
**Dependencies**: jsPDF, docx libraries

---

### 2.5 Predictive Analytics

**Component**: `PredictiveDashboard.jsx`

**Prediction features**:
1. **Congestion prediction**:
   - Congestion forecast for next 15 / 30 / 60 minutes
   - Based on historical patterns and current trends

2. **Optimal signal timing prediction**:
   - Suggested green-time adjustments
   - Expected efficiency improvement

3. **What-if analysis tool**:
   - "What if 50 more vehicles are added?"
   - "What if Edge-5 is closed?"
   - Real-time simulation of predicted outcomes

**Backend model**:
```python
# Python microservice (Flask)
from flask import Flask, request, jsonify
from statsmodels.tsa.arima.model import ARIMA
import numpy as np

app = Flask(__name__)

@app.route('/predict/congestion', methods=['POST'])
def predict_congestion():
    data = request.json['historical_data']
    model = ARIMA(data, order=(5,1,0))
    model_fit = model.fit()
    forecast = model_fit.forecast(steps=12)  # Next 60 minutes (5-min intervals)
    return jsonify({'predictions': forecast.tolist()})
```

**Effort**: 6 days
**Priority**: P1
**Dependencies**: Python microservice, ARIMA / LSTM models

---

## Phase 3: Long-term Extensions

### 3.1 Multi-city Support

**Architecture change**:
```
frontend/src/data/cities/
|-- arlington_va.json
|-- boston_ma.json
|-- austin_tx.json
`-- custom/
    `-- user_city_1.json
```

**City configuration format**:
```json
{
  "cityId": "arlington_va",
  "cityName": "Arlington, Virginia",
  "nodes": [...],
  "edges": [...],
  "defaultConfig": {
    "signalMode": "ADAPTIVE",
    "speedLimitMultiplier": 1.0,
    "capacityMultiplier": 1.0
  },
  "mapBounds": {
    "minX": 0,
    "maxX": 1400,
    "minY": 0,
    "maxY": 900
  }
}
```

**City selector**:
```javascript
const CitySelector = () => {
  const [selectedCity, setSelectedCity] = useState('arlington_va');

  const handleCityChange = async (cityId) => {
    const cityData = await import(`../data/cities/${cityId}.json`);
    await api.post('/simulation/load-city', cityData);
    setSelectedCity(cityId);
  };

  return (
    <select onChange={(e) => handleCityChange(e.target.value)}>
      <option value="arlington_va">Arlington, VA</option>
      <option value="boston_ma">Boston, MA</option>
      <option value="austin_tx">Austin, TX</option>
      <option value="custom">Custom Map</option>
    </select>
  );
};
```

**Custom map import**:
- GeoJSON format support
- Automatic coordinate system conversion
- Node / edge validation

**Effort**: 8 days
**Priority**: P3

---

### 3.2 Machine Learning Model Integration

**Architecture**:
```
+-------------+
|  React UI   |
+------+------+
       |
+------+--------+
|  Spring Boot  |
+------+--------+
       | HTTP
+------+--------+
|  ML Service   |
|  (Python)     |
|  Flask/FastAPI|
+---------------+
```

**ML model types**:

#### 3.2.1 Reinforcement Learning Signal Control
```python
import gym
import numpy as np
from stable_baselines3 import DQN

class TrafficSignalEnv(gym.Env):
    def __init__(self, intersection_data):
        self.state_space = gym.spaces.Box(...)  # Queue lengths, waiting times
        self.action_space = gym.spaces.Discrete(4)  # NS-Green, EW-Green, etc.

    def step(self, action):
        # Apply signal action
        # Calculate reward = -total_waiting_time
        return next_state, reward, done, info

# Train model
env = TrafficSignalEnv(intersection_data)
model = DQN('MlpPolicy', env, verbose=1)
model.learn(total_timesteps=100000)
model.save('signal_control_dqn')
```

#### 3.2.2 Congestion Prediction Neural Network
```python
import tensorflow as tf
from tensorflow.keras import layers

model = tf.keras.Sequential([
    layers.LSTM(64, input_shape=(12, 5)),  # 12 timesteps, 5 features
    layers.Dense(32, activation='relu'),
    layers.Dense(1, activation='sigmoid')  # Congestion probability
])

model.compile(optimizer='adam', loss='binary_crossentropy')
model.fit(X_train, y_train, epochs=50)
```

#### 3.2.3 Traffic Pattern Clustering
```python
from sklearn.cluster import KMeans

# Cluster traffic patterns
patterns = np.array([...])  # Shape: (n_samples, n_features)
kmeans = KMeans(n_clusters=5)
kmeans.fit(patterns)

# Identify pattern: Morning Rush, Evening Rush, Weekend, etc.
```

**Frontend integration**:
```javascript
const MLControlPanel = () => {
  const [modelType, setModelType] = useState('DQN');
  const [isTraining, setIsTraining] = useState(false);

  const trainModel = async () => {
    setIsTraining(true);
    const response = await fetch('http://localhost:5000/train', {
      method: 'POST',
      body: JSON.stringify({ model: modelType, episodes: 1000 })
    });
    const result = await response.json();
    setIsTraining(false);
  };

  return (
    <div>
      <select value={modelType} onChange={e => setModelType(e.target.value)}>
        <option value="DQN">Deep Q-Network</option>
        <option value="A3C">Asynchronous Actor-Critic</option>
        <option value="PPO">Proximal Policy Optimization</option>
      </select>
      <button onClick={trainModel} disabled={isTraining}>
        {isTraining ? 'Training...' : 'Train Model'}
      </button>
    </div>
  );
};
```

**Effort**: 15 days
**Priority**: P3

---

### 3.3 Mobile Application Version

**Technology stack**: React Native + Expo

**Status**: Cancelled — focus on desktop optimization

**Reason**: To provide a better desktop user experience the system has been optimized for full-width layout, and mobile responsive design has been removed.

**Directory structure**:
```
mobile/
|-- src/
|   |-- screens/
|   |   |-- MapScreen.js
|   |   |-- MetricsScreen.js
|   |   `-- SettingsScreen.js
|   |-- components/
|   `-- services/
|-- app.json
`-- package.json
```

**Effort**: 20 days
**Priority**: P3

---

### 3.4 Cloud Deployment

**AWS deployment architecture**:
```
                  +-------------+
                  | CloudFront  |
                  |   (CDN)     |
                  +------+------+
                         |
                  +------+------+
                  |  S3 Bucket  |
                  |  (Frontend) |
                  +-------------+

+-------------+          +-------------+
|   Route 53  |----------+     ALB     |
|    (DNS)    |          | (Load Bal.) |
+-------------+          +------+------+
                                |
                    +-----------+-----------+
                    |                       |
             +------+------+        +------+------+
             |  EC2 / ECS  |        |  EC2 / ECS  |
             |  (Backend)  |        |  (Backend)  |
             +------+------+        +------+------+
                    |                      |
                    +----------+-----------+
                               |
                        +------+------+
                        |  RDS (PG)   |
                        |  + ElastiC  |
                        |  (Redis)    |
                        +-------------+
```

**Deployment steps**:
1. **Dockerize the backend**:
```dockerfile
FROM openjdk:18-jdk-slim
COPY target/traffic-optimization-0.0.1-SNAPSHOT.jar app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

2. **Build and upload frontend to S3**:
```bash
npm run build
aws s3 sync dist/ s3://traffic-optimization-frontend
```

3. **Configure RDS database**:
```sql
CREATE DATABASE traffic_optimization;
```

4. **Set up ElastiCache (Redis)**:
```java
@Configuration
public class RedisConfig {
    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        return new LettuceConnectionFactory(redisHost, redisPort);
    }
}
```

**CI/CD pipeline**:
```yaml
# .github/workflows/deploy.yml
name: Deploy to AWS
on:
  push:
    branches: [main]
jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      - name: Build Backend
        run: mvn clean package
      - name: Build Docker Image
        run: docker build -t traffic-backend .
      - name: Push to ECR
        run: |
          aws ecr get-login-password | docker login --username AWS --password-stdin
          docker push 123456789.dkr.ecr.us-east-1.amazonaws.com/traffic-backend
      - name: Deploy to ECS
        run: aws ecs update-service --cluster traffic-cluster --service backend --force-new-deployment
```

**Effort**: 10 days
**Priority**: P3

---

### 3.5 Public API and Third-party Integration

**API endpoints**:
```
POST   /api/v1/auth/token                 # Get API key
GET    /api/v1/simulations                # List all simulations
POST   /api/v1/simulations                # Create new simulation
GET    /api/v1/simulations/{id}/metrics   # Get metrics
POST   /api/v1/simulations/{id}/flows     # Create traffic flow
DELETE /api/v1/simulations/{id}           # Delete simulation
```

**Authentication**:
```java
@Configuration
public class SecurityConfig extends WebSecurityConfigurerAdapter {
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.authorizeRequests()
            .antMatchers("/api/v1/**").authenticated()
            .and()
            .addFilterBefore(new ApiKeyAuthFilter(), UsernamePasswordAuthenticationFilter.class);
    }
}
```

**Rate limiting**:
```java
@Component
public class RateLimiter {
    private final Map<String, AtomicInteger> requestCounts = new ConcurrentHashMap<>();

    public boolean allowRequest(String apiKey) {
        AtomicInteger count = requestCounts.computeIfAbsent(apiKey, k -> new AtomicInteger(0));
        return count.incrementAndGet() <= 100; // 100 requests per minute
    }
}
```

**Webhook**:
```java
@Service
public class WebhookService {
    public void sendWebhook(String url, WebhookEvent event) {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.postForEntity(url, event, String.class);
    }
}
```

**SDK example (JavaScript)**:
```javascript
// traffic-optimization-sdk
class TrafficOptimizationClient {
  constructor(apiKey) {
    this.apiKey = apiKey;
    this.baseURL = 'https://api.traffic-optimization.com/v1';
  }

  async createSimulation(config) {
    const response = await fetch(`${this.baseURL}/simulations`, {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${this.apiKey}`,
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(config)
    });
    return response.json();
  }

  async getMetrics(simulationId) {
    const response = await fetch(`${this.baseURL}/simulations/${simulationId}/metrics`, {
      headers: { 'Authorization': `Bearer ${this.apiKey}` }
    });
    return response.json();
  }
}
```

**Effort**: 8 days
**Priority**: P3

---

## Phase 4: Performance Optimization

### 4.1 Frontend Performance Optimization

**Optimization items**:

#### 4.1.1 Virtual Scrolling
```javascript
import { FixedSizeList } from 'react-window';

const VirtualNodeList = ({ nodes }) => {
  const Row = ({ index, style }) => (
    <div style={style}>
      {nodes[index].id} - {nodes[index].name}
    </div>
  );

  return (
    <FixedSizeList
      height={400}
      itemCount={nodes.length}
      itemSize={50}
      width="100%"
    >
      {Row}
    </FixedSizeList>
  );
};
```

#### 4.1.2 WebGL Map Rendering
```javascript
import * as PIXI from 'pixi.js';

const MapVisualizationWebGL = ({ graphData }) => {
  const app = new PIXI.Application({ width: 1400, height: 900 });

  graphData.edges.forEach(edge => {
    const line = new PIXI.Graphics();
    line.lineStyle(2, 0x3B82F6);
    line.moveTo(edge.fromNode.x, edge.fromNode.y);
    line.lineTo(edge.toNode.x, edge.toNode.y);
    app.stage.addChild(line);
  });

  // Render vehicles as sprites for better performance
  graphData.vehicles.forEach(vehicle => {
    const sprite = PIXI.Sprite.from('car.png');
    sprite.x = vehicle.x;
    sprite.y = vehicle.y;
    app.stage.addChild(sprite);
  });
};
```

#### 4.1.3 Code Splitting
```javascript
import { lazy, Suspense } from 'react';

const MapVisualization = lazy(() => import('./components/MapVisualization'));
const AdvancedAnalytics = lazy(() => import('./components/AdvancedAnalytics'));

function App() {
  return (
    <Suspense fallback={<LoadingSpinner />}>
      <MapVisualization />
      <AdvancedAnalytics />
    </Suspense>
  );
}
```

#### 4.1.4 Memoization
```javascript
import { useMemo, memo } from 'react';

const MetricsDisplay = memo(({ metrics }) => {
  const formattedMetrics = useMemo(() => {
    return Object.entries(metrics).map(([key, value]) => ({
      key,
      value: value.toFixed(2)
    }));
  }, [metrics]);

  return <div>{/* Render formatted metrics */}</div>;
});
```

#### 4.1.5 Web Workers
```javascript
// efficiencyWorker.js
self.onmessage = (e) => {
  const { vehicles, edges } = e.data;

  const efficiency = vehicles.reduce((sum, v) => {
    return sum + (v.distance / v.travelTime);
  }, 0) / vehicles.length;

  self.postMessage({ efficiency });
};

// In component
const worker = new Worker('efficiencyWorker.js');
worker.postMessage({ vehicles, edges });
worker.onmessage = (e) => {
  setEfficiency(e.data.efficiency);
};
```

**Target metrics**:
- Frame rate > 60 FPS
- Initial load < 1 second
- Per-frame time < 16ms

**Effort**: 6 days
**Priority**: P1

---

### 4.2 Backend Performance Optimization

#### 4.2.1 Redis Caching
```java
@Service
public class GraphDataService {
    @Autowired
    private RedisTemplate<String, Graph> redisTemplate;

    @Cacheable(value = "graph", key = "#cityId")
    public Graph getGraph(String cityId) {
        // Load from database or file
        return graphRepository.findByCityId(cityId);
    }
}
```

#### 4.2.2 Database Indexes
```sql
CREATE INDEX idx_vehicle_simulation ON vehicles(simulation_id);
CREATE INDEX idx_metrics_timestamp ON metrics_snapshots(simulation_id, timestamp);
CREATE INDEX idx_events_type ON system_events(event_type, timestamp);
```

#### 4.2.3 Batch Updates
```java
@Service
public class VehicleUpdateService {
    private final List<VehicleUpdate> updateBuffer = new ArrayList<>();

    @Scheduled(fixedRate = 1000) // Every 1 second
    public void flushUpdates() {
        if (updateBuffer.isEmpty()) return;

        // Batch update
        vehicleRepository.saveAll(updateBuffer);

        // Single WebSocket broadcast
        messagingTemplate.convertAndSend("/topic/vehicles", updateBuffer);

        updateBuffer.clear();
    }
}
```

#### 4.2.4 Connection Pool Tuning
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
```

#### 4.2.5 Response Compression
```java
@Configuration
public class CompressionConfig {
    @Bean
    public FilterRegistrationBean<GzipFilter> gzipFilter() {
        FilterRegistrationBean<GzipFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new GzipFilter());
        registration.addUrlPatterns("/api/*");
        return registration;
    }
}
```

**Target metrics**:
- API response < 50ms
- Support 10,000 vehicles
- WebSocket latency < 20ms

**Effort**: 5 days
**Priority**: P1

---

## Phase 5: User Experience Enhancements

### 5.1 Theme System

**Implementation**:
```javascript
// ThemeProvider.jsx
import { createContext, useState, useEffect } from 'react';

export const ThemeContext = createContext();

const themes = {
  light: {
    bg: '#FFFFFF',
    text: '#1F2937',
    primary: '#667eea',
    secondary: '#764ba2'
  },
  dark: {
    bg: '#1F2937',
    text: '#F9FAFB',
    primary: '#818CF8',
    secondary: '#A78BFA'
  },
  highContrast: {
    bg: '#000000',
    text: '#FFFFFF',
    primary: '#FFFF00',
    secondary: '#00FFFF'
  }
};

export const ThemeProvider = ({ children }) => {
  const [theme, setTheme] = useState('light');

  useEffect(() => {
    const savedTheme = localStorage.getItem('theme') || 'light';
    setTheme(savedTheme);
  }, []);

  const switchTheme = (newTheme) => {
    setTheme(newTheme);
    localStorage.setItem('theme', newTheme);

    // Apply CSS variables
    Object.entries(themes[newTheme]).forEach(([key, value]) => {
      document.documentElement.style.setProperty(`--${key}`, value);
    });
  };

  return (
    <ThemeContext.Provider value={{ theme, switchTheme }}>
      {children}
    </ThemeContext.Provider>
  );
};
```

**Effort**: 2 days
**Priority**: P0

---

### 5.2 Customizable Dashboard

**Using react-grid-layout**:
```javascript
import GridLayout from 'react-grid-layout';
import 'react-grid-layout/css/styles.css';

const DashboardBuilder = () => {
  const [layout, setLayout] = useState([
    { i: 'map', x: 0, y: 0, w: 8, h: 6 },
    { i: 'metrics', x: 8, y: 0, w: 4, h: 3 },
    { i: 'chart', x: 8, y: 3, w: 4, h: 3 }
  ]);

  const onLayoutChange = (newLayout) => {
    setLayout(newLayout);
    localStorage.setItem('dashboard_layout', JSON.stringify(newLayout));
  };

  return (
    <GridLayout
      className="layout"
      layout={layout}
      cols={12}
      rowHeight={100}
      width={1400}
      onLayoutChange={onLayoutChange}
      draggableHandle=".drag-handle"
    >
      <div key="map" className="widget">
        <div className="drag-handle">Map</div>
        <MapVisualization />
      </div>
      <div key="metrics" className="widget">
        <div className="drag-handle">Metrics</div>
        <MetricsDisplay />
      </div>
      <div key="chart" className="widget">
        <div className="drag-handle">Chart</div>
        <MetricsChart />
      </div>
    </GridLayout>
  );
};
```

**Effort**: 4 days
**Priority**: P2

---

### 5.3 Keyboard Shortcuts

**Implementation**:
```javascript
import { useEffect } from 'react';

const useKeyboardShortcuts = ({ onPlay, onPause, onReset, onExport }) => {
  useEffect(() => {
    const handleKeyPress = (e) => {
      // Check if input is focused
      if (e.target.tagName === 'INPUT' || e.target.tagName === 'TEXTAREA') {
        return;
      }

      switch(e.key) {
        case ' ':
          e.preventDefault();
          isPlaying ? onPause() : onPlay();
          break;
        case 'r':
          onReset();
          break;
        case 'e':
          onExport();
          break;
        case 'h':
          toggleHeatmap();
          break;
        case '?':
          showShortcutHelp();
          break;
        default:
          break;
      }

      // Ctrl/Cmd + Z
      if ((e.ctrlKey || e.metaKey) && e.key === 'z') {
        e.preventDefault();
        undoLastAction();
      }
    };

    window.addEventListener('keydown', handleKeyPress);
    return () => window.removeEventListener('keydown', handleKeyPress);
  }, [onPlay, onPause, onReset, onExport]);
};
```

**Effort**: 1 day
**Priority**: P2

---

### 5.4 Onboarding Tutorial

**Using react-joyride**:
```javascript
import Joyride from 'react-joyride';

const OnboardingWizard = () => {
  const [run, setRun] = useState(false);

  const steps = [
    {
      target: '.control-panel',
      content: 'Click here to start, pause, and reset the simulation.',
    },
    {
      target: '.traffic-flow-panel',
      content: 'Create new traffic flows here.',
    },
    {
      target: '.map-visualization',
      content: 'The map shows real-time vehicle positions and road network state.',
    },
    {
      target: '.heatmap-overlay',
      content: 'The heatmap shows road congestion levels.',
    },
    {
      target: '.statistics-dashboard',
      content: 'The dashboard shows key performance metrics.',
    }
  ];

  useEffect(() => {
    const hasSeenTutorial = localStorage.getItem('tutorial_completed');
    if (!hasSeenTutorial) {
      setRun(true);
    }
  }, []);

  const handleJoyrideCallback = (data) => {
    if (data.status === 'finished' || data.status === 'skipped') {
      localStorage.setItem('tutorial_completed', 'true');
      setRun(false);
    }
  };

  return (
    <Joyride
      steps={steps}
      run={run}
      continuous
      showSkipButton
      callback={handleJoyrideCallback}
      styles={{
        options: {
          primaryColor: '#667eea',
          zIndex: 10000,
        }
      }}
    />
  );
};
```

**Effort**: 2 days
**Priority**: P2

---

### 5.5 Accessibility Improvements

**ARIA labels**:
```javascript
<button
  aria-label="Start simulation"
  aria-pressed={isRunning}
  onClick={handleStart}
>
  {isRunning ? 'Pause' : 'Start'}
</button>

<div
  role="region"
  aria-label="Traffic map visualization"
  aria-live="polite"
>
  <MapVisualization />
</div>
```

**Keyboard navigation**:
```css
button:focus,
input:focus {
  outline: 3px solid #667eea;
  outline-offset: 2px;
}

.panel {
  &:focus-within {
    box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.5);
  }
}
```

**Color-blind-friendly palette**:
```javascript
const colorBlindPalette = {
  // Okabe-Ito palette
  blue: '#0173B2',
  orange: '#DE8F05',
  green: '#029E73',
  yellow: '#ECE133',
  red: '#CC3311',
  purple: '#949494'
};
```

**Effort**: 3 days
**Priority**: P1

---

### 5.6 Internationalization (i18n)

**Using react-i18next**:
```javascript
// i18n.js
import i18n from 'i18next';
import { initReactI18next } from 'react-i18next';

import enUS from './locales/en-US.json';
import zhCN from './locales/zh-CN.json';
import esES from './locales/es-ES.json';

i18n
  .use(initReactI18next)
  .init({
    resources: {
      'en-US': { translation: enUS },
      'zh-CN': { translation: zhCN },
      'es-ES': { translation: esES }
    },
    lng: 'en-US',
    fallbackLng: 'en-US',
    interpolation: {
      escapeValue: false
    }
  });

export default i18n;
```

**Locale file example**:
```json
// locales/zh-CN.json
{
  "simulation": {
    "start": "Start",
    "pause": "Pause",
    "reset": "Reset",
    "status": "Status"
  },
  "metrics": {
    "efficiency": "Network Efficiency",
    "speed": "Average Speed",
    "congestion": "Congestion Level"
  }
}
```

**Usage**:
```javascript
import { useTranslation } from 'react-i18next';

const ControlPanel = () => {
  const { t, i18n } = useTranslation();

  return (
    <div>
      <button onClick={handleStart}>{t('simulation.start')}</button>
      <button onClick={handlePause}>{t('simulation.pause')}</button>

      <select onChange={(e) => i18n.changeLanguage(e.target.value)}>
        <option value="en-US">English</option>
        <option value="zh-CN">Chinese</option>
        <option value="es-ES">Spanish</option>
      </select>
    </div>
  );
};
```

**Effort**: 4 days
**Priority**: P2

---

## Phase 6: Advanced Features

### 6.1 Incident Simulation

**Component**: `IncidentManager.jsx`

**Features**:
```javascript
const IncidentManager = ({ edges, onIncidentCreate }) => {
  const [incidents, setIncidents] = useState([]);

  const createIncident = async (incidentData) => {
    const incident = {
      id: Date.now(),
      type: incidentData.type, // ACCIDENT, CONSTRUCTION, CLOSURE
      edgeId: incidentData.edgeId,
      startTime: new Date(),
      duration: incidentData.duration, // minutes
      severity: incidentData.severity // 0.0 to 1.0
    };

    // Disable edge or reduce capacity
    await api.post('/simulation/incidents', incident);
    setIncidents([...incidents, incident]);
  };

  return (
    <div>
      <h3>Active Incidents</h3>
      {incidents.map(incident => (
        <IncidentCard key={incident.id} incident={incident} />
      ))}
      <button onClick={() => createIncident({
        type: 'ACCIDENT',
        edgeId: 'Edge-5',
        duration: 30,
        severity: 0.8
      })}>
        Simulate Accident
      </button>
    </div>
  );
};
```

**Backend**:
```java
@Service
public class IncidentService {
    public void applyIncident(Incident incident) {
        Edge edge = graphService.getEdge(incident.getEdgeId());

        if (incident.getType() == IncidentType.CLOSURE) {
            edge.setEnabled(false);
        } else {
            // Reduce capacity
            double reduction = incident.getSeverity();
            edge.setCapacityMultiplier(1.0 - reduction);
        }

        // Schedule removal after duration
        scheduler.schedule(
            () -> removeIncident(incident),
            incident.getDuration(),
            TimeUnit.MINUTES
        );
    }
}
```

**Effort**: 4 days
**Priority**: P2

---

### 6.2 Weather Conditions

**Component**: `WeatherSimulator.jsx`

**Weather effects**:
```javascript
const weatherEffects = {
  CLEAR: {
    speedMultiplier: 1.0,
    capacityMultiplier: 1.0,
    accidentProbability: 0.01
  },
  RAIN: {
    speedMultiplier: 0.8,
    capacityMultiplier: 0.9,
    accidentProbability: 0.03
  },
  SNOW: {
    speedMultiplier: 0.6,
    capacityMultiplier: 0.7,
    accidentProbability: 0.05
  },
  FOG: {
    speedMultiplier: 0.7,
    capacityMultiplier: 0.85,
    accidentProbability: 0.04
  }
};

const WeatherSimulator = () => {
  const [weather, setWeather] = useState('CLEAR');

  const applyWeather = async (weatherType) => {
    await api.post('/simulation/weather', {
      type: weatherType,
      effects: weatherEffects[weatherType]
    });
    setWeather(weatherType);
  };

  return (
    <div>
      <select value={weather} onChange={(e) => applyWeather(e.target.value)}>
        <option value="CLEAR">Clear</option>
        <option value="RAIN">Rain</option>
        <option value="SNOW">Snow</option>
        <option value="FOG">Fog</option>
      </select>

      {weather !== 'CLEAR' && (
        <WeatherOverlay type={weather} />
      )}
    </div>
  );
};
```

**Effort**: 3 days
**Priority**: P3

---

## Priority Matrix

| Priority | Feature | Impact | Effort | ROI |
|----------|---------|--------|--------|-----|
| **P0** | Notification system | High | Low | ***** |
| **P0** | Event log | High | Low | ***** |
| **P0** | Theme system | Medium | Low | **** |
| **P1** | Route replay | High | Medium | ***** |
| **P1** | Advanced analytics | High | High | **** |
| **P1** | Frontend performance | High | Medium | ***** |
| **P1** | Backend performance | High | Medium | ***** |
| **P1** | Accessibility | Medium | Low | **** |
| **P2** | Multi-scenario comparison | Medium | High | *** |
| **P2** | Report generator | Medium | Medium | *** |
| **P2** | Customizable dashboard | Medium | Medium | *** |
| **P2** | Incident simulation | Medium | Low | *** |
| **P3** | Machine learning integration | Very High | Very High | **** |
| **P3** | Multi-city support | High | High | *** |
| **P3** | Cloud deployment | High | Medium | **** |
| **P3** | Mobile app | Medium | Very High | ** |
| **P3** | Public API | Medium | Medium | *** |

---

## Implementation Timeline

### Months 1-2: Foundation Strengthening
- **Week 1-2**: Notification system + event log + theme system
- **Week 3-4**: Advanced map controls + historical comparison
- **Week 5-8**: Frontend and backend performance optimization

### Months 3-4: Advanced Features
- **Week 9-12**: Route replay + advanced analytics dashboard
- **Week 13-16**: Multi-scenario comparison + report generator

### Months 5-6: Intelligence Layer
- **Week 17-20**: Predictive analytics + ML microservice
- **Week 21-24**: Multi-city support + API development

### Month 7+: Scale and Polish
- Mobile application development
- Cloud deployment
- Collaboration features
- Advanced simulation (weather, incidents, transit)

---

## Success Metrics

### System Performance
- Support 10,000+ concurrent vehicles
- API response time < 50ms
- Frontend frame time < 16ms
- System uptime 99.9%

### User Experience
- Initial load time < 2 seconds
- WCAG 2.1 AA accessibility compliance
- Mobile responsive: removed — desktop focus
- New user onboarding time < 5 minutes
- Full-width desktop display optimization

### Business Value
- 20%+ efficiency improvement over baseline
- Support for 5+ custom city configurations
- API capacity 1,000+ requests/minute
- 100+ reports exported per month

---

## Closing Remarks

This roadmap provides a comprehensive extension path for the Traffic Signal Optimization System, from immediate enhancements to long-term vision. Each phase builds on the previous work, ensuring backward compatibility while adding significant new capabilities.

**Current status**: Phase 0 (core system) — all components complete
**Recommended next step**: Start with Phase 1, prioritizing the notification system and event log viewer

---

**Document version**: 1.0
**Last updated**: 2025-03-15
**Authors**: Chengkun Liao, Mingjie Shen
**Project**: INFO6205 Final Project
