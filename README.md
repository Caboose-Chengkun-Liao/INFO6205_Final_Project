# Traffic Signal Optimization System

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Java](https://img.shields.io/badge/Java-18-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.0-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-18-blue.svg)](https://reactjs.org/)

**INFO6205 Program Structure and Algorithms - Final Project**

**Authors**: Chengkun Liao, Mingjie Shen

**Semester**: Spring 2025 | **School**: Northeastern University

---

## Table of Contents

- [Project Overview](#project-overview)
- [System Features](#system-features)
- [Technology Stack](#technology-stack)
- [Quick Start](#quick-start)
- [Project Structure](#project-structure)
- [Core Modules](#core-modules)
- [API Documentation](#api-documentation)
- [Efficiency Formula](#efficiency-formula)
- [Documentation Guide](#documentation-guide)
- [Screenshots](#screenshots)
- [Contributing](#contributing)
- [License](#license)

---

## Project Overview

The Traffic Signal Optimization System is an **enterprise-grade traffic simulation and optimization platform** built on real road network data from Arlington, Virginia. It uses intelligent algorithms to optimize signal control, improve urban traffic efficiency, and reduce congestion and wait times.

### Core Value

- **Smart Signal Control** - Three optimization modes (Fixed / Adaptive / Green Wave), dynamically adjusting signal timing
- **Real-time Performance Monitoring** - 8 key KPI metrics for comprehensive traffic state assessment
- **Interactive Visualization** - SVG map rendering, real-time vehicle animation, heatmap overlay
- **Data-driven Decisions** - Export CSV/JSON data for in-depth analysis
- **Scientific Evaluation** - Quantitative assessment based on the efficiency formula

### Use Cases

- **Urban Traffic Planning** - Evaluate the impact of different signal control strategies on traffic flow
- **Teaching and Research** - Practical project for algorithms and data structures courses
- **Academic Research** - Traffic engineering and intelligent transportation systems research
- **Decision Support** - Optimization recommendations for traffic management authorities

---

## System Features

### Completed Features (v1.0)

#### Backend (Spring Boot 3.2.0 + Java 18)
- **Graph Data Structure** - Weighted directed graph representing the road network (20 nodes, 48 edges)
- **Shortest Path Algorithm** - Dijkstra algorithm for optimal routing
- **Traffic Flow Simulation Engine** - Discrete event simulation supporting concurrent vehicles
- **Dynamic Speed Adjustment** - Congestion slowdown based on road occupancy rate
  - Occupancy > 90%: speed reduced to 30% (severe congestion)
  - Occupancy > 75%: speed reduced to 50% (heavy congestion)
  - Occupancy > 50%: speed reduced to 75% (moderate congestion)
  - Occupancy > 25%: speed reduced to 90% (light congestion)
- **Signal Control System** - Three modes: FIXED / ADAPTIVE / GREEN_WAVE
- **Performance Metric Calculation** - 8 KPIs including efficiency, speed, and congestion
- **WebSocket Real-time Push** - SockJS + STOMP protocol, low-latency data transmission
- **RESTful API** - Full CRUD endpoints with CORS support
- **JSON Serialization Optimization** - Circular reference prevention for improved performance

#### Frontend (React 18 + Vite)
- **Map Visualization (MapVisualization)** - Optimized SVG rendering (1600x800px)
  - Desktop-optimized, full-width layout showing the full Arlington road network
  - Real-time vehicle animation with smooth visual effects
- **Control Panel (ControlPanel)** - Start / pause / reset simulation
- **Traffic Flow Management (TrafficFlowPanel)** - Create vehicle flows with form validation
- **Performance Monitor (PerformanceMonitor)** - Real-time KPI display with trend charts
- **Signal Control (SignalControlPanel)** - Mode switching and status monitoring
- **Node Search (NodeSearchPanel)** - Quick lookup with type filtering
- **Statistics Dashboard (StatisticsDashboard)** - 8 KPI cards with performance summary
- **Data Export (DataExportPanel)** - CSV/JSON format, 5 data types
- **Heatmap Overlay (TrafficHeatmapOverlay)** - Three modes: congestion / speed / flow

### Real-time Update Mechanism
- **WebSocket Push** - Simulation state and metric data (< 50ms latency)
- **Polling Updates** - Efficiency trend (5 seconds), heatmap data (2 seconds)
- **HMR Hot Reload** - Frontend code changes take effect immediately

### User Experience
- **Floating Panel Design** - 9 functional modules, independently expandable/collapsible
- **Desktop Optimized** - Node hover, click selection, full map display
- **Visual Feedback** - Color coding (green/yellow/red), animated transitions
- **Data Visualization** - Line charts, progress bars, heatmaps
- **Congestion Simulation** - Real-time display of road occupancy and vehicle speed changes

---

## Technology Stack

### Backend Technologies

| Technology | Version | Purpose |
|------------|---------|---------|
| **Java** | 18 | Core development language (must be 18; Java 25 is not supported) |
| **Spring Boot** | 3.2.0 | Web application framework |
| **Spring Web** | 6.1.1 | RESTful API |
| **Spring WebSocket** | 6.1.1 | Real-time bi-directional communication |
| **Lombok** | 1.18.30 | Boilerplate reduction |
| **Jackson** | 2.15.3 | JSON serialization |
| **Maven** | 3.6+ | Project build |

**Key Configuration**:
```xml
<!-- Jackson circular reference solution -->
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
@JsonIgnoreProperties({"trafficLight", "incomingEdges", "outgoingEdges"})
```

### Frontend Technologies

| Technology | Version | Purpose |
|------------|---------|---------|
| **React** | 18.2.0 | UI framework |
| **Vite** | 5.0.0 | Build tool + HMR |
| **Axios** | 1.6.2 | HTTP client |
| **SockJS Client** | 1.6.1 | WebSocket client |
| **STOMP.js** | 2.3.3 | WebSocket message protocol |
| **CSS-in-JS** | - | Inline styling system |

**Developer Experience**:
- Vite fast build (< 1s startup)
- Hot Module Replacement (HMR)
- Component-based architecture
- Code splitting (future optimization)

### Data Structures and Algorithms

```java
// Weighted directed graph
Graph {
  List<Node> nodes;      // 20 nodes (12 intersections + 8 boundaries)
  List<Edge> edges;      // 48 directed edges
}

// Dijkstra shortest path
class DijkstraAlgorithm {
  Map<String, Double> distances;
  Map<String, String> previous;
  PriorityQueue<Node> queue;
}

// Efficiency calculation
E = Sigma(Ni x Li / ti) / Sigma(Ni)
```

---

## Quick Start

### System Requirements

**Software**:
- **Java 18** (JDK 18.0.2.1) - Required; Lombok does not support Java 25
- **Node.js** 16+ (18.x recommended)
- **Maven** 3.6+
- **Browser** Chrome 90+ / Firefox 88+ / Safari 14+ / Edge 90+

**Hardware**:
- CPU: Dual-core 2.0 GHz+
- RAM: 4 GB (8 GB recommended)
- Disk: 500 MB free space

### Installation

#### 1. Clone the Repository

```bash
git clone https://github.com/your-username/INFO6205_Final_Project.git
cd INFO6205_Final_Project
```

#### 2. Start the Backend

```bash
cd backend

# Ensure Java 18 is active
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-18.0.2.1.jdk/Contents/Home
java -version  # Verify version

# Build and run
mvn clean install
mvn spring-boot:run -DskipTests
```

**Success indicator**:
```
2025-03-15 10:30:00.123  INFO 12345 --- [main] c.t.o.TrafficOptimizationApplication : Started TrafficOptimizationApplication in 5.234 seconds (JVM running for 5.678)
```

Backend runs at **http://localhost:8080**

#### 3. Start the Frontend

Open a new terminal window:

```bash
cd frontend

# Install dependencies (first run only)
npm install

# Start development server
npm run dev
```

**Success indicator**:
```
  VITE v5.0.0  ready in 1234 ms

  Local:   http://localhost:5174/
  Network: use --host to expose
  press h to show help
```

Frontend runs at **http://localhost:5174**

#### 4. Open the Application

Navigate to `http://localhost:5174` in your browser.

**Connection status check**:
- Top right shows **Connected** -> WebSocket connected successfully
- Top right shows **Disconnected** -> Check that the backend service is running

---

## Project Structure

```
INFO6205_Final_Project/
|-- README.md                     # Project overview (this file)
|-- ROADMAP.md                    # Extension roadmap
|-- USER_GUIDE.md                 # User guide
|-- SETUP_GUIDE.md                # Environment setup guide
|-- ARLINGTON_MAP.md              # Arlington road network data
|-- CLAUDE.md                     # AI assistant guidance
|
|-- backend/                      # Backend project (Spring Boot)
|   |-- src/main/java/com/traffic/optimization/
|   |   |-- algorithm/            # Algorithm implementations
|   |   |   `-- DijkstraAlgorithm.java
|   |   |-- config/               # Configuration classes
|   |   |   |-- GraphConfig.java      # Road network data initialization
|   |   |   |-- WebSocketConfig.java  # WebSocket configuration
|   |   |   `-- CorsConfig.java       # CORS configuration
|   |   |-- controller/           # REST API controllers
|   |   |   `-- SimulationController.java
|   |   |-- model/                # Data models
|   |   |   |-- Node.java             # Node
|   |   |   |-- Edge.java             # Edge
|   |   |   |-- TrafficFlow.java      # Traffic flow
|   |   |   |-- Vehicle.java          # Vehicle
|   |   |   |-- TrafficLight.java     # Traffic light
|   |   |   `-- SimulationMetrics.java # Metrics
|   |   |-- service/              # Business logic
|   |   |   |-- GraphService.java
|   |   |   |-- SimulationService.java
|   |   |   |-- TrafficFlowService.java
|   |   |   `-- MetricsService.java
|   |   |-- websocket/            # WebSocket handling
|   |   |   `-- SimulationWebSocketHandler.java
|   |   `-- TrafficOptimizationApplication.java
|   |-- src/main/resources/
|   |   `-- application.properties
|   |-- pom.xml                   # Maven dependency configuration
|   `-- target/                   # Compiled output
|
`-- frontend/                     # Frontend project (React + Vite)
    |-- src/
    |   |-- components/           # React components
    |   |   |-- MapVisualization.jsx        # Map visualization
    |   |   |-- ControlPanel.jsx            # Simulation control
    |   |   |-- TrafficFlowPanel.jsx        # Traffic flow management
    |   |   |-- PerformanceMonitor.jsx      # Performance monitoring
    |   |   |-- SignalControlPanel.jsx      # Signal control
    |   |   |-- NodeSearchPanel.jsx         # Node search
    |   |   |-- StatisticsDashboard.jsx     # Statistics dashboard
    |   |   |-- DataExportPanel.jsx         # Data export
    |   |   |-- TrafficHeatmapOverlay.jsx   # Heatmap
    |   |   |-- MetricsDisplay.jsx          # Metrics display
    |   |   `-- MetricsChart.jsx            # Charts
    |   |-- services/             # API services
    |   |   |-- api.js                # HTTP requests
    |   |   `-- websocket.js          # WebSocket client
    |   |-- App.jsx               # Main application component
    |   |-- App.css               # Styles
    |   `-- main.jsx              # Entry point
    |-- public/                   # Static assets
    |-- package.json              # npm dependency configuration
    |-- vite.config.js            # Vite configuration
    `-- dist/                     # Production build output
```

---

## Core Modules

### 1. Map Visualization (MapVisualization)

**Function**: Interactive SVG map showing the road network and vehicles in real time

**Technical highlights**:
- SVG rendering (1400x900 viewBox)
- Zoom / pan interaction
- Vehicle animation (CSS transition)
- Node / edge hover tooltips

**Code example**:
```jsx
<svg viewBox="0 0 1400 900">
  {/* Render edges */}
  {edges.map(edge => (
    <line x1={edge.fromNode.x} y1={edge.fromNode.y}
          x2={edge.toNode.x} y2={edge.toNode.y} />
  ))}

  {/* Render vehicles */}
  {vehicles.map(vehicle => (
    <circle cx={vehicle.x} cy={vehicle.y} r={5} fill="#10B981" />
  ))}
</svg>
```

---

### 2. Statistics Dashboard (StatisticsDashboard)

**8 key metrics**:

| Metric | Formula / Description | Target |
|--------|-----------------------|--------|
| Network Efficiency | E = Sigma(Ni x Li / ti) / Sigma(Ni) | >= 40 km/h |
| Average Speed | Mean speed of all vehicles | >= 50 km/h |
| Active Vehicles | Total vehicles currently in the network | - |
| Completed Journeys | Vehicles that reached their destination | - |
| Total Distance | Cumulative distance traveled (km) | - |
| Avg Travel Time | Average trip duration (minutes) | Lower is better |
| Network Congestion | Mean segment capacity utilization (%) | < 40% |
| System Throughput | Vehicles x speed / 60 (vehicles/min) | - |

**Real-time updates**:
- WebSocket push: `/topic/simulation`
- Poll interval: 3 seconds
- Color coding: green = good, yellow = moderate, red = poor

---

### 3. Traffic Heatmap Overlay (TrafficHeatmapOverlay)

**Three visualization modes**:

#### Congestion Mode
```
Congestion rate = (current vehicles / segment capacity) x 100%

Color mapping:
< 25%  - free flow
25-50% - light congestion
50-70% - moderate congestion
70-85% - heavy congestion
> 85%  - severe congestion
```

#### Speed Mode
```
Speed ratio = (current speed / speed limit) x 100%

Color mapping:
> 80% - fast
60-80% - medium
40-60% - slow
20-40% - very slow
< 20%  - stopped
```

#### Flow Volume Mode
```
Flow ratio = (current flow / capacity) x 100%

Color mapping:
< 30%  - low volume
30-60% - medium volume
60-80% - high volume
> 80%  - very high volume
```

---

### 4. Data Export (DataExportPanel)

**Supported data types**:

| Data Type | Formats | Content |
|-----------|---------|---------|
| Performance Metrics | CSV / JSON | Efficiency, speed, travel time, etc. |
| Traffic Flows | CSV / JSON | Traffic flow records (origin, destination, vehicle count) |
| Signal States | CSV / JSON | Signal light state history |
| Network Graph | JSON | Complete road network topology |
| Efficiency Trend | CSV / JSON | Most recent 100 efficiency data points |

**CSV example**:
```csv
averageEfficiency,averageSpeed,totalVehicles,completedFlows
42.5,55.3,120,45
```

**JSON example**:
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

## API Documentation

### REST API Endpoints

**Base URL**: `http://localhost:8080/api`

#### Simulation Control

```bash
# Start simulation
POST /simulation/start
Response: { "status": "RUNNING", "message": "Simulation started" }

# Pause simulation
POST /simulation/pause
Response: { "status": "PAUSED" }

# Reset simulation
POST /simulation/reset
Response: { "status": "STOPPED", "message": "Simulation reset" }

# Get simulation status
GET /simulation/status
Response: { "state": "RUNNING", "elapsedTime": 123456 }
```

#### Data Retrieval

```bash
# Get graph data
GET /simulation/graph
Response: {
  "nodes": [...],
  "edges": [...]
}

# Get performance metrics
GET /simulation/metrics
Response: {
  "averageEfficiency": 42.5,
  "averageSpeed": 55.3,
  "totalVehicles": 120,
  ...
}

# Get efficiency trend
GET /simulation/efficiency/trend?count=50
Response: [
  { "timestamp": "2025-03-15T10:30:00", "efficiency": 42.5 },
  { "timestamp": "2025-03-15T10:30:05", "efficiency": 43.1 },
  ...
]

# Get vehicle list
GET /simulation/vehicles
Response: [
  { "id": "VEH-001", "x": 350.5, "y": 450.2, "speed": 55.0 },
  ...
]
```

#### Traffic Flow Management

```bash
# Create traffic flow
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

# Get all traffic flows
GET /simulation/flows
Response: [
  { "id": "FLOW-001", "entryPoint": "B-1", "destination": "B-3", "numberOfCars": 20 },
  ...
]
```

#### Signal Control

```bash
# Switch signal mode
POST /simulation/signals/mode?mode=ADAPTIVE
Response: { "mode": "ADAPTIVE", "message": "Signal mode changed" }

# Get signal states
GET /simulation/signals
Response: [
  { "nodeId": "I-1", "state": "GREEN", "remainingTime": 15 },
  { "nodeId": "I-2", "state": "RED", "remainingTime": 25 },
  ...
]
```

### WebSocket Endpoint

**Connection URL**: `ws://localhost:8080/ws`

**Using the SockJS client**:

```javascript
import SockJS from 'sockjs-client';
import { Stomp } from '@stomp/stompjs';

const socket = new SockJS('http://localhost:8080/ws');
const stompClient = Stomp.over(socket);

stompClient.connect({}, () => {
  // Subscribe to simulation updates (pushed every second)
  stompClient.subscribe('/topic/simulation', (message) => {
    const data = JSON.parse(message.body);
    console.log('Simulation data:', data);
  });

  // Subscribe to performance metrics (pushed every 5 seconds)
  stompClient.subscribe('/topic/metrics', (message) => {
    const metrics = JSON.parse(message.body);
    console.log('Metrics data:', metrics);
  });
});
```

**Message formats**:

```json
// /topic/simulation
{
  "metrics": { /* performance metrics */ },
  "vehicles": [ /* vehicle list */ ],
  "edges": [ /* segment states */ ]
}

// /topic/metrics
{
  "averageEfficiency": 42.5,
  "averageSpeed": 55.3,
  "timestamp": "2025-03-15T10:30:00"
}
```

---

## Efficiency Formula

### Mathematical Definition

$$
E = \frac{\sum_{i=1}^{n} (N_i \times L_i / t_i)}{\sum_{i=1}^{n} N_i}
$$

Where:
- **E**: Efficiency value (km/h)
- **N_i**: Number of vehicles in traffic flow i
- **L_i**: Total road length of traffic flow i (km)
- **t_i**: Travel time of traffic flow i (hours)
- **n**: Total number of traffic flows

### Java Implementation

```java
public double calculateEfficiency() {
    double sumWeightedSpeed = 0.0;
    int totalVehicles = 0;

    for (TrafficFlow flow : activeFlows) {
        int vehicles = flow.getNumberOfCars();
        double distance = flow.getTotalDistance(); // km
        double travelTime = flow.getTravelTime() / 3600.0; // convert to hours

        if (travelTime > 0) {
            sumWeightedSpeed += vehicles * (distance / travelTime);
            totalVehicles += vehicles;
        }
    }

    return totalVehicles > 0 ? sumWeightedSpeed / totalVehicles : 0.0;
}
```

### Optimization Goal

By adjusting the signal control strategy, **minimize travel time t_i** to **maximize efficiency E**.

**Optimization strategies**:
- **FIXED mode**: Fixed-duration signals (30s green / 30s red)
- **ADAPTIVE mode**: Dynamically adjusts based on real-time queue length using Webster's formula
- **GREEN_WAVE mode**: Bi-directional corridor coordination using Little (1966) offset method

**Expected improvement**:
- ADAPTIVE vs FIXED: **10-15% efficiency gain**
- GREEN_WAVE vs FIXED: **20-30% efficiency gain**

---

## Documentation Guide

### Complete Document Set

| Document | Description | Audience |
|----------|-------------|----------|
| [README.md](README.md) | Project overview and quick start (this file) | All users |
| [USER_GUIDE.md](USER_GUIDE.md) | Detailed user guide | End users |
| [ROADMAP.md](ROADMAP.md) | Future extension roadmap | Developers / managers |
| [SETUP_GUIDE.md](SETUP_GUIDE.md) | Environment setup and troubleshooting | Developers |
| [ARLINGTON_MAP.md](ARLINGTON_MAP.md) | Arlington road network data details | Researchers |
| [CLAUDE.md](CLAUDE.md) | AI assistant collaboration guide | AI developers |

### Quick Links

- **New user?** Start with [Quick Start](#quick-start)
- **Detailed operations?** See [USER_GUIDE.md](USER_GUIDE.md)
- **Environment issues?** See [SETUP_GUIDE.md](SETUP_GUIDE.md)
- **Future plans?** See [ROADMAP.md](ROADMAP.md)
- **API calls?** See [API Documentation](#api-documentation)

---

## Screenshots

### Main Interface
```
+------------------------------------------------------------+
|  Traffic Signal Optimization System        [Connected]     |
+------------------------------------------------------------+
|                                                            |
|  [Start] [Pause] [Reset]               State: RUNNING     |
|                                                            |
+------------------------------------------------------------+
|                                                            |
|  +------------------------------------------------------+ |
|  | Real-time System Statistics              [LIVE]      | |
|  +------------------------------------------------------+ |
|  | Efficiency: 42.5 km/h   Speed: 55.3 km/h            | |
|  | Vehicles: 120           Completed: 45               | |
|  | Distance: 24.5 km       Travel time: 5.2 min        | |
|  | Congestion: 35%         Throughput: 110 v/min       | |
|  +------------------------------------------------------+ |
|                                                            |
|  +------------------------------------------------------+ |
|  |                 Map + Heatmap                        | |
|  |                                                      | |
|  |    I-1 ---green---> I-2 ---yellow---> I-3           | |
|  |     |                 |                 |            | |
|  |    green            yellow             red           | |
|  |     |                 |                 |            | |
|  |    B-1              I-5               B-3            | |
|  |                                                      | |
|  |  [Search] [Flow] [Monitor] [Signal] [Export]        | |
|  +------------------------------------------------------+ |
|                                                            |
|  +------------------------------------------------------+ |
|  |              Efficiency Trend                        | |
|  |  50 km/h |                          /\              | |
|  |  40 km/h |               /\        /  \            | |
|  |  30 km/h |      /\      /  \      /    \           | |
|  |  20 km/h +------  ------    ------       -------   | |
|  |           0s    10s   20s   30s   40s   50s        | |
|  +------------------------------------------------------+ |
|                                                            |
+------------------------------------------------------------+
|  Traffic Signal Optimization System - INFO6205 Final      |
|  Authors: Chengkun Liao, Mingjie Shen                     |
+------------------------------------------------------------+
```

> Note: The actual interface is a graphical UI; the above is an ASCII art illustration.

### Core Interface Components

1. **Map View** - SVG-rendered Arlington road network, 20 nodes, 48 edges
2. **Statistics Cards** - 8 KPIs updated in real time with color coding
3. **Heatmap** - Three switchable modes: congestion / speed / flow
4. **Trend Chart** - Efficiency time-series visualization
5. **Floating Panels** - 9 functional modules with independent interaction

---

## Contributing

### How to Contribute

All forms of contribution are welcome!

**Ways to contribute**:
- Report bugs
- Suggest new features
- Improve documentation
- Submit code

### Reporting Bugs

1. Visit [GitHub Issues](https://github.com/your-username/INFO6205_Final_Project/issues)
2. Click "New Issue"
3. Select the "Bug Report" template
4. Provide the following information:
   - OS and browser version
   - Java version (`java -version`)
   - Node.js version (`node -v`)
   - Steps to reproduce
   - Expected behavior vs actual behavior
   - Error logs or screenshots

### Submitting Code

```bash
# 1. Fork the repository
# 2. Create a feature branch
git checkout -b feature/your-feature-name

# 3. Commit changes
git add .
git commit -m "Add: your feature description"

# 4. Push to the branch
git push origin feature/your-feature-name

# 5. Create a Pull Request
```

**Code standards**:
- Java: follow Google Java Style Guide
- JavaScript: use ESLint + Prettier
- Component naming: PascalCase (e.g. `MapVisualization.jsx`)
- Function naming: camelCase (e.g. `calculateEfficiency()`)
- Comments: required for all key logic

### Development Roadmap

See [ROADMAP.md](ROADMAP.md) for:
- Phase 1: Immediate enhancements (1-3 days)
- Phase 2: Mid-term features (1-2 weeks)
- Phase 3: Long-term extensions (future plans)

---

## License

This project is released under the **MIT License**.

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

## Acknowledgments

### Team Members

- **Chengkun Liao** - Backend development, algorithm implementation
- **Mingjie Shen** - Frontend development, UI/UX design

### Technical Credits

- **Spring Boot Team** - Powerful backend framework
- **React Team** - Modern frontend library
- **Vite Team** - Ultra-fast build tooling
- **Northeastern University** - Learning environment and resources

### References

- [Dijkstra's Algorithm](https://en.wikipedia.org/wiki/Dijkstra%27s_algorithm)
- [Traffic Flow Theory](https://en.wikipedia.org/wiki/Traffic_flow)
- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [React Documentation](https://react.dev/)

---

## Contact

**Course**: INFO6205 - Program Structure and Algorithms

**Semester**: Spring 2025

**School**: Northeastern University

**Repository**: [GitHub](https://github.com/your-username/INFO6205_Final_Project)

**Feedback**: [your-email@northeastern.edu]

---

**Last Updated**: 2025-03-15

**Version**: 1.0.0

**Status**: Production Ready

---

<div align="center">

**Making Urban Traffic Smarter**

Made with love by Chengkun Liao & Mingjie Shen

</div>
