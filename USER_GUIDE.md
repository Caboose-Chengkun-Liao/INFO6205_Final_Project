# Traffic Signal Optimization System - User Guide

**Version**: 1.0
**Last Updated**: 2025-03-15
**Authors**: Chengkun Liao, Mingjie Shen
**Project**: INFO6205 Final Project

---

## Table of Contents

1. [System Overview](#system-overview)
2. [Quick Start](#quick-start)
3. [Feature Module Details](#feature-module-details)
4. [Common Workflows](#common-workflows)
5. [Advanced Features](#advanced-features)
6. [Troubleshooting](#troubleshooting)
7. [FAQ](#faq)
8. [Glossary](#glossary)

---

## System Overview

### What is the Traffic Signal Optimization System?

The Traffic Signal Optimization System is a traffic simulation and optimization platform built on real road network data. It models the road network of Arlington, Virginia, USA, and uses visualizations of vehicle movement, signal control, and traffic flow to help users:

- **Optimize signal timing** — reduce wait times and improve throughput efficiency
- **Analyze traffic performance** — monitor key metrics in real time (speed, congestion, efficiency)
- **Visualize road network state** — intuitively view road congestion levels
- **Export simulation data** — CSV / JSON formats for further analysis

### System Architecture

```
+-----------------+         +-----------------+
|  Frontend UI    |  HTTP   |  Backend Server |
|  (React)        |<------->|  (Spring Boot)  |
|  localhost:5174 |WebSocket|  localhost:8080 |
+-----------------+         +-----------------+
```

- **Frontend**: React 18 + Vite — interactive visualization interface
- **Backend**: Spring Boot 3.2.0 + Java 18 — simulation logic and data management
- **Communication**: REST API + WebSocket real-time bi-directional communication

### Core Features

| Module | Description |
|--------|-------------|
| Map Visualization | Interactive road network map rendered with SVG (1600x800px) |
| Dynamic Speed Adjustment | Congestion slowdown based on occupancy rate |
| Simulation Control | Start / pause / reset the simulation |
| Traffic Flow Management | Create and manage vehicle flows |
| Performance Monitoring | Real-time KPI metrics display |
| Signal Light Control | Switch between optimization modes |
| Node Search | Quickly locate intersections and boundary points |
| Statistics Dashboard | 8 key performance indicators |
| Data Export | CSV / JSON format export |
| Heatmap Overlay | Congestion / speed / flow visualization |

---

## Quick Start

### System Requirements

**Software**:
- Node.js 16+ (18.x recommended)
- Java 18 (required; Java 25 is not supported)
- Maven 3.6+
- Modern browser (Chrome 90+, Firefox 88+, Safari 14+, Edge 90+)

**Hardware**:
- CPU: dual-core 2.0 GHz or higher
- RAM: 4 GB minimum (8 GB recommended)
- Disk: 500 MB free space

### Installation

#### 1. Clone the Repository

```bash
git clone https://github.com/your-repo/INFO6205_Final_Project.git
cd INFO6205_Final_Project
```

#### 2. Start the Backend

```bash
cd backend

# Ensure Java 18 is active
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-18.0.2.1.jdk/Contents/Home

# Build and run
mvn clean install
mvn spring-boot:run -DskipTests
```

**Success indicator**:
```
2025-03-15 10:30:00.123  INFO 12345 --- [main] c.t.o.TrafficOptimizationApplication : Started TrafficOptimizationApplication in 5.234 seconds
```

Backend runs at `http://localhost:8080`

#### 3. Start the Frontend

Open a new terminal window:

```bash
cd frontend

# Install dependencies (first run only)
npm install

# Start the development server
npm run dev
```

**Success indicator**:
```
  VITE v8.0.0  ready in 1234 ms

  Local:   http://localhost:5174/
  Network: use --host to expose
```

Frontend runs at `http://localhost:5174`

#### 4. Open the Application

Navigate to `http://localhost:5174` in your browser.

**Connection status check**:
- Top right shows **Connected** (green dot) — WebSocket connected successfully
- If it shows **Disconnected** (grey dot), check that the backend is running

---

## Feature Module Details

### 1. Control Panel

**Location**: Top-left of the page

**Features**:
- **Start simulation**: click the "Start" button
- **Pause simulation**: click the "Pause" button
- **Reset simulation**: click the "Reset" button — clears all vehicles and data

**Status indicators**:
- **RUNNING** — simulation is running
- **PAUSED** — simulation is paused
- **STOPPED** — simulation has stopped

**Usage example**:
1. Click "Start" to start the simulation
2. Watch vehicles move on the map
3. Click "Pause" to freeze and inspect the current state
4. Click "Resume" to continue
5. Click "Reset" to begin a new simulation

---

### 2. Map Visualization

**Location**: Main center area of the page

**Map size**: 1600x800 pixels, desktop-optimized, full-width layout

**Element guide**:

#### Nodes
- **Blue circles** — intersections
- **Red squares** — boundary entry / exit points
- Nodes are labeled with their IDs (e.g. "I-1", "B-1")

#### Edges
- **Blue lines** — road connections
- **Arrows** — indicate one-way direction
- Line thickness represents road capacity

#### Vehicles
- **Small green dots** — moving vehicles
- Vehicles move along road edges

#### Road State (Dynamic Speed Adjustment)
- **Congestion affects speed** — higher road occupancy means slower vehicles:
  - Occupancy > 90%: speed reduced to 30% (severe congestion, red)
  - Occupancy > 75%: speed reduced to 50% (heavy congestion, orange)
  - Occupancy > 50%: speed reduced to 75% (moderate congestion, yellow)
  - Occupancy > 25%: speed reduced to 90% (light congestion)
  - Occupancy < 25%: normal speed (free flow, green)

#### Interaction
- **Node click**: click a node to open its detail panel
- **Node hover**: hover to show node name and type
- **Edge hover**: hover to show road distance, capacity, and load

**Tips**:
- Watch road color changes to judge congestion levels
- Click a node to see the traffic light state at that intersection
- Vehicle movement speed gives a visceral sense of traffic fluency

---

### 3. Traffic Flow Creation Panel

**Location**: Bottom-right floating button (purple, vehicle icon)

**Purpose**: Create a vehicle flow from an origin to a destination

**Steps**:

1. **Click the floating button** to open the panel

2. **Select origin**:
   - Choose a boundary point from the dropdown (e.g. "B-1")
   - Only BOUNDARY-type nodes can serve as origins

3. **Select destination**:
   - Choose any node from the dropdown (e.g. "B-3")

4. **Set vehicle count**:
   - Use the slider: 1-50 vehicles
   - Or type a number directly

5. **Click "Create Flow"**

**Success indicator**:
- Green notification: "Traffic flow created successfully!"
- Vehicles will begin spawning at the origin

**Failure reasons**:
- Origin and destination are the same
- No valid path exists
- Simulation has not been started

**Best practices**:
- Start the simulation before creating traffic flows
- Try different origin / destination combinations to observe routing differences
- Gradually increase vehicle count to watch congestion form

---

### 4. Performance Monitor Panel

**Location**: Right-side floating button (green, chart icon)

**Two display modes**:

#### Compact View
- Shows current network efficiency value
- Trend indicator:
  - Up arrow — rising (green)
  - Down arrow — falling (red)
  - Right arrow — steady (grey)

#### Expanded View
Click the floating button to expand; shows 6 metrics:

| Metric | Description | Unit |
|--------|-------------|------|
| Network Efficiency | Overall network efficiency | km/h |
| Average Speed | Mean vehicle speed | km/h |
| Total Vehicles | Current total vehicles in network | vehicles |
| Avg Travel Time | Average trip duration | seconds |
| Total Distance | Cumulative distance traveled | km |
| Active Flows | Number of active traffic flows | flows |

**Efficiency trend chart**:
- X-axis: time
- Y-axis: efficiency value (km/h)
- Shows the most recent 50 data points
- Updates every 5 seconds

**Color coding**:
- Green: performance is good
- Yellow: performance is moderate
- Red: performance is poor

**Use cases**:
- Observe efficiency changes when switching signal modes
- Compare performance metrics at different traffic loads
- Export trend data for offline analysis

---

### 5. Signal Light Control Panel

**Location**: Right-side floating button (blue, signal icon)

**Three signal control modes**:

#### FIXED (Fixed Timing)
- **Description**: Fixed red/green light durations
- **Green duration**: 30 seconds
- **Red duration**: 30 seconds
- **Best for**: roads with uniform, predictable traffic

#### ADAPTIVE (Adaptive)
- **Description**: Dynamically adjusts based on real-time queue lengths using Webster's formula
- **Adjustment strategy**: directions with longer queues get priority green
- **Response speed**: fast (evaluated every 10 seconds)
- **Best for**: intersections with high traffic variability

#### GREEN_WAVE (Green Wave)
- **Description**: Corridor-coordinated offsets so vehicles traveling at design speed never stop
- **Optimization goal**: minimize stops along the main corridor
- **Method**: Little (1966) offset calculation
- **Best for**: multi-intersection main arterials

**Switching modes**:
1. Click the floating button to open the panel
2. Click the desired mode button
3. The system applies the new mode automatically
4. Watch for changes in the efficiency metric

**Signal state display**:
- Current signal state of each intersection (GREEN / RED)
- Remaining time countdown

**Experiment recommendation**:
- Use FIXED mode as a baseline
- Switch to ADAPTIVE and observe improvement
- Try GREEN_WAVE mode last
- Record efficiency values for each mode to compare

---

### 6. Node Search Panel

**Location**: Left-side floating button (orange, search icon)

**Purpose**: Quickly find and locate road network nodes

**Search features**:
- **Search by ID**: type "I-1" to find intersection 1
- **Search by name**: type the node name (if assigned)
- **Live filtering**: results update instantly while typing

**Type filter**:
- **All**: show all nodes (20 total)
- **Intersections**: show only intersections (12 total)
- **Boundaries**: show only boundary points (8 total)

**Node card information**:
```
I-1                      <- ID
Intersection             <- Type description
INTERSECTION             <- Type label (blue)
X: 350.00  Y: 450.00    <- Coordinates
```

**Interaction**:
1. Click a node card
2. The map automatically centers on that node
3. The node is highlighted
4. The `onNodeSelect` callback fires in `App.jsx`

**Use cases**:
- Quickly locate a specific intersection
- View the exact coordinates of a node
- Filter out all boundary points as traffic flow origins

---

### 7. Statistics Dashboard

**Location**: Top of the page (above the map)

**8 Key KPIs**:

#### Network Efficiency
- **Definition**: overall system operational efficiency
- **Formula**: E = Sigma(Ni x Li / ti) / Sigma(Ni)
  - Ni = vehicle count
  - Li = travel distance
  - ti = travel time
- **Target**: >= 40 km/h (excellent)

#### Average Speed
- **Definition**: mean travel speed of all vehicles
- **Unit**: km/h
- **Target**: >= 50 km/h (good)

#### Active Vehicles
- **Definition**: total vehicles currently in the road network
- **Includes**: moving vehicles + queuing vehicles

#### Completed Journeys
- **Definition**: total vehicles that have reached their destination
- **Purpose**: measures system throughput

#### Total Distance
- **Definition**: cumulative distance traveled by all vehicles
- **Unit**: km

#### Avg Travel Time
- **Definition**: average time from departure to arrival
- **Unit**: minutes
- **Target**: lower is better

#### Network Congestion
- **Definition**: overall network congestion level
- **Calculation**: weighted average congestion rate across all segments
- **Range**: 0-100%
- **Ratings**:
  - < 40% — free flow (green)
  - 40-70% — moderate congestion (yellow)
  - > 70% — severe congestion (red)

#### System Throughput
- **Definition**: vehicle processing capacity per unit time
- **Unit**: vehicles / minute
- **Formula**: (total vehicles x average speed) / 60

**Real-time updates**:
- WebSocket push: immediate
- Poll interval: every 3 seconds
- Animated: smooth value transitions

**Performance summary cards**:
- **Network Health**: Excellent / Good / Fair
- **Traffic Flow**: Smooth / Congested
- **System Status**: Operational / Error

**Optimization goal progress bars**:
- Efficiency target: current efficiency / 50 km/h
- Congestion reduction: 100% - current congestion level

---

### 8. Data Export Panel

**Location**: Bottom-right floating button (purple, disk icon)

**5 data types**:

#### Performance Metrics
- **Content**: efficiency, speed, travel time, etc.
- **Formats**: CSV / JSON
- **Filename**: `traffic_metrics_[timestamp].csv`

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
  "completedFlows": 45
}
```

#### Traffic Flows
- **Content**: all created traffic flow records
- **Fields**: origin, destination, vehicle count, creation time

#### Signal States
- **Content**: signal light state history for each intersection
- **Fields**: intersection ID, signal state, timestamp

#### Network Graph
- **Content**: complete node and edge data
- **Format**: JSON (includes coordinates, capacity, etc.)
- **Use**: import into other analysis tools

#### Efficiency Trend
- **Content**: most recent 100 efficiency data points
- **Formats**: CSV / JSON
- **Use**: draw trend charts, time-series analysis

**Export steps**:
1. Click the floating button to open the panel
2. Choose a data type
3. Click "CSV" or "JSON"
4. The file automatically downloads to your browser's download folder

**Export all data**:
- Click "Export All Data (JSON)"
- Exports all 5 data types in one JSON file
- Includes an export timestamp

**File naming convention**:
```
traffic_metrics_1710489600000.csv
traffic_flows_1710489600000.json
traffic_data_complete_1710489600000.json  <- all data
```

**Use cases**:
- Import data into Excel for further analysis
- Save simulation results for reports
- Share simulation data with others
- Batch-process results from multiple simulation runs

---

### 9. Traffic Heatmap Overlay

**Location**: Top-right floating control panel

**3 visualization modes**:

#### Congestion Mode
- **Data source**: segment capacity utilization
- **Formula**: (current vehicles / segment capacity) x 100%
- **Color mapping**:
  - Green (< 25%) — free flow
  - Light green (25-50%) — light congestion
  - Yellow (50-70%) — moderate congestion
  - Red (70-85%) — heavy congestion
  - Dark red (> 85%) — severe congestion

#### Speed Mode
- **Data source**: segment actual speed vs speed limit
- **Formula**: (current speed / speed limit) x 100%
- **Color mapping**:
  - Green (> 80%) — fast
  - Light green (60-80%) — medium speed
  - Yellow (40-60%) — slow
  - Red (20-40%) — very slow
  - Dark red (< 20%) — stopped

#### Flow Volume Mode
- **Data source**: segment vehicle count
- **Formula**: (current flow / capacity) x 100%
- **Color mapping**:
  - Green (< 30%) — low volume
  - Light green (30-60%) — medium volume
  - Yellow (60-80%) — high volume
  - Red (> 80%) — very high volume

**Controls**:
- **ON/OFF toggle**: show or hide the heatmap
- **Mode selection**: click a button to switch mode
- **Show/Hide Legend**: click "Show/Hide Legend"

**Visual effects**:
- Segment colors update in real time
- Congested segments use thicker lines (6px)
- Smooth transition animation (0.5 seconds)
- Pulse animation (3-second period)

**Tips**:
- Use Congestion mode to identify bottleneck segments
- Use Speed mode to check traffic fluency
- Use Flow Volume mode to evaluate network load balance
- Compare heatmaps under different signal modes

**Real-time updates**:
- WebSocket subscription: `/topic/simulation`
- Poll interval: every 2 seconds

---

## Common Workflows

### Workflow 1: Run a Complete Simulation

**Goal**: run a complete traffic simulation from scratch

**Steps**:

1. **Start the system**
   ```bash
   # Terminal 1: start backend
   cd backend
   mvn spring-boot:run -DskipTests

   # Terminal 2: start frontend
   cd frontend
   npm run dev
   ```

2. **Open a browser**
   - Navigate to `http://localhost:5174`
   - Check the connection status in the top-right (should show "Connected")

3. **Start the simulation**
   - Click "Start" in the control panel
   - State changes to RUNNING

4. **Create a traffic flow**
   - Click the purple traffic flow button
   - Origin: B-1
   - Destination: B-3
   - Vehicle count: 20
   - Click "Create Flow"

5. **Observe**
   - Green vehicle dots appear on the map
   - Vehicles move along their path
   - Statistics dashboard numbers begin changing

6. **Monitor performance**
   - Click the green performance monitor button
   - View real-time metrics
   - Watch the efficiency trend chart

7. **Switch signal mode**
   - Click the blue signal light button
   - Switch to ADAPTIVE mode
   - Observe efficiency metric changes

8. **Export data**
   - Click the purple data export button
   - Select "Performance Metrics"
   - Click "CSV" to download

9. **End the simulation**
   - Click "Pause"
   - Click "Reset"

**Expected results**:
- Vehicles successfully travel from B-1 to B-3
- Efficiency settles between 30-50 km/h
- CSV file downloads successfully

---

### Workflow 2: Compare Signal Modes

**Goal**: compare the performance of FIXED, ADAPTIVE, and GREEN_WAVE modes

**Steps**:

1. **Prepare a record table**
   ```
   | Mode        | Avg Efficiency | Avg Speed | Avg Travel Time |
   |-------------|----------------|-----------|-----------------|
   | FIXED       |                |           |                 |
   | ADAPTIVE    |                |           |                 |
   | GREEN_WAVE  |                |           |                 |
   ```

2. **Test FIXED mode**
   - Reset the simulation
   - Switch to FIXED mode
   - Start the simulation
   - Create traffic flow: B-1 -> B-3, 30 vehicles
   - Wait 5 minutes
   - Record final metrics
   - Export data as `fixed_mode.csv`

3. **Test ADAPTIVE mode**
   - Reset the simulation
   - Switch to ADAPTIVE mode
   - Repeat the above steps
   - Export data as `adaptive_mode.csv`

4. **Test GREEN_WAVE mode**
   - Reset the simulation
   - Switch to GREEN_WAVE mode
   - Repeat the above steps
   - Export data as `greenwave_mode.csv`

5. **Analyze data**
   - Open the three CSV files in Excel
   - Create comparison charts
   - Calculate improvement percentages

**Expected results**:
- GREEN_WAVE mode achieves the highest corridor efficiency
- ADAPTIVE mode improves 10-15% over FIXED
- GREEN_WAVE improves 20-30% over FIXED

---

### Workflow 3: Identify and Analyze Bottleneck Segments

**Goal**: find congestion bottlenecks in the road network and analyze their causes

**Steps**:

1. **Enable the heatmap**
   - Open the heatmap panel
   - Ensure it is ON
   - Select "Congestion" mode

2. **Create a high-traffic scenario**
   - Create multiple traffic flows:
     - B-1 -> B-3, 20 vehicles
     - B-2 -> B-4, 20 vehicles
     - B-5 -> B-7, 15 vehicles
   - Start the simulation

3. **Observe the heatmap**
   - Wait 2-3 minutes
   - Identify red / dark-red segments (> 70% congestion)
   - Record bottleneck segment IDs

4. **Use node search**
   - Open the node search panel
   - Search for the start and end nodes of the bottleneck segment
   - Click a node to locate it

5. **Analyze causes**
   - Check the segment's:
     - Capacity (capacityPerKm)
     - Current vehicle count (currentVehicleCount)
     - Signal light state
     - Upstream / downstream conditions

6. **Switch to speed mode**
   - Switch the heatmap to "Speed" mode
   - Observe speed reduction on the bottleneck segment

7. **Try optimization**
   - Switch signal mode to ADAPTIVE or GREEN_WAVE
   - Observe whether the bottleneck eases
   - Record the improvement

8. **Generate a report**
   - Export efficiency trend data
   - Screenshot the heatmap
   - Write an analysis conclusion

**Example bottleneck analysis**:
```
Segment: Edge-12 (I-3 -> I-5)
Capacity: 50 vehicles
Current vehicles: 45
Congestion rate: 90%
Cause: downstream intersection I-5 has an excessively long red-light phase
Recommendation: shorten red-light duration or increase segment capacity
```

---

### Workflow 4: Export a Complete Report

**Goal**: generate a report containing all simulation data

**Steps**:

1. **Run a stable simulation**
   - Create 3-5 traffic flows
   - Run for at least 10 minutes
   - Ensure metrics are stable

2. **Screenshot key views**
   - Full map (showing all vehicles)
   - Statistics dashboard
   - Heatmap (congestion mode)
   - Performance monitor panel

3. **Export all data**
   - Open the data export panel
   - Click "Export All Data"
   - Save file: `traffic_data_complete_[timestamp].json`

4. **Export individual data types**
   - Performance Metrics -> CSV
   - Efficiency Trend -> CSV
   - Traffic Flows -> JSON
   - Network Graph -> JSON

5. **Organize files**
   ```
   report_folder/
   |-- screenshots/
   |   |-- map_view.png
   |   |-- dashboard.png
   |   `-- heatmap.png
   |-- data/
   |   |-- complete_data.json
   |   |-- metrics.csv
   |   |-- trend.csv
   |   |-- flows.json
   |   `-- graph.json
   `-- report.docx
   ```

6. **Write the report document**
   - **Part 1**: Simulation Configuration
     - Road network: Arlington (20 nodes, 48 edges)
     - Signal mode: GREEN_WAVE
     - Run duration: 10 minutes
   - **Part 2**: Performance Metrics
     - Table showing all 8 KPIs
     - Insert screenshots
   - **Part 3**: Trend Analysis
     - Import CSV data into Excel
     - Draw efficiency trend line chart
   - **Part 4**: Bottleneck Analysis
     - Insert heatmap screenshot
     - Annotate congested segments
   - **Part 5**: Optimization Recommendations
     - Propose improvements based on data

7. **Export PDF**
   - Export the Word document as PDF
   - Final report: `traffic_simulation_report_[date].pdf`

---

## Advanced Features

### Customize Road Network Data

**Location**: `backend/src/main/java/com/traffic/optimization/config/GraphConfig.java`

**Add a node**:
```java
// Add a new node
Node newNode = new Node("I-13", NodeType.INTERSECTION, 800.0, 600.0);
newNode.setName("New Intersection");
nodes.add(newNode);
```

**Add an edge**:
```java
// Add a new road
Edge newEdge = new Edge("Edge-49", nodeI13, nodeI5, 1.5, 60.0, 50.0);
edges.add(newEdge);
```

**Recompile**:
```bash
mvn clean install
mvn spring-boot:run -DskipTests
```

---

### WebSocket Message Subscription

**Frontend subscription example**:
```javascript
import websocketService from './services/websocket';

// Subscribe to simulation updates
websocketService.subscribe('/topic/simulation', (data) => {
  console.log('Simulation data:', data);
  // data.metrics - performance metrics
  // data.vehicles - vehicle list
  // data.edges - segment states
});

// Subscribe to performance metrics
websocketService.subscribe('/topic/metrics', (data) => {
  console.log('Metrics data:', data);
});
```

---

### REST API Calls

**Base URL**: `http://localhost:8080/api`

**Main endpoints**:

```bash
# Get graph data
GET /simulation/graph

# Get current metrics
GET /simulation/metrics

# Create traffic flow
POST /simulation/flows
Content-Type: application/json
{
  "entryPoint": "B-1",
  "destination": "B-3",
  "numberOfCars": 20
}

# Switch signal mode
POST /simulation/signals/mode?mode=ADAPTIVE

# Get efficiency trend
GET /simulation/efficiency/trend?count=50
```

**Testing with curl**:
```bash
# Get graph data
curl http://localhost:8080/api/simulation/graph

# Create traffic flow
curl -X POST http://localhost:8080/api/simulation/flows \
  -H "Content-Type: application/json" \
  -d '{"entryPoint":"B-1","destination":"B-3","numberOfCars":20}'
```

---

## Troubleshooting

### Problem 1: Backend fails to start

**Error**:
```
[ERROR] Failed to execute goal org.apache.maven.plugins:maven-compiler-plugin:3.11.0:compile
```

**Cause**: Java version mismatch

**Solution**:
```bash
# Check Java version
java -version

# Should show: openjdk version "18.0.2.1"

# If not, set JAVA_HOME
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-18.0.2.1.jdk/Contents/Home
```

---

### Problem 2: Frontend shows "Disconnected"

**Cause**: WebSocket connection failed

**Diagnostic steps**:

1. **Confirm backend is running**
   ```bash
   curl http://localhost:8080/api/simulation/graph
   # Should return JSON data
   ```

2. **Check the browser console**
   - Open DevTools (F12)
   - Check the Network tab
   - Look for the WebSocket connection (ws://localhost:8080/ws)

3. **Check CORS configuration**
   - File: `backend/src/main/java/com/traffic/optimization/config/WebSocketConfig.java`
   - Confirm the frontend address is included:
     ```java
     .setAllowedOrigins("http://localhost:5174")
     ```

4. **Restart services**
   ```bash
   # Stop backend (Ctrl+C)
   # Restart
   mvn spring-boot:run -DskipTests
   ```

---

### Problem 3: Vehicles are not moving

**Possible causes**:

1. **Simulation not started**
   - Check control panel state
   - Should show RUNNING

2. **No traffic flows created**
   - Open the traffic flow panel
   - Create at least one flow

3. **No valid path exists**
   - Check origin and destination
   - Ensure a valid path exists between them

**Debug steps**:
```bash
# Watch backend logs
# Should see messages like:
# "Created traffic flow: B-1 -> B-3 with 20 vehicles"
# "Vehicle VEH-001 spawned at B-1"
# "Vehicle VEH-001 moving on Edge-5"
```

---

### Problem 4: Data export fails

**Error**: "Export failed: Network Error"

**Solutions**:

1. **Check the backend API**
   ```bash
   curl http://localhost:8080/api/simulation/metrics
   ```

2. **Check the browser console**
   - Any CORS errors?
   - Any 404 errors?

3. **Clear browser cache**
   - Hard refresh (Ctrl+Shift+R)

4. **Check file permissions**
   - Ensure the browser can download files to the default folder

---

### Problem 5: Heatmap not showing

**Symptom**: no color changes on the map

**Checklist**:

- [ ] Is the heatmap toggle ON?
- [ ] Are there vehicles running in the network?
- [ ] Any JavaScript errors in the browser console?
- [ ] Is graphData loaded correctly?

**Debug code**:
```javascript
// Run in the browser console
console.log(window.graphData); // Should display node and edge data
```

---

## FAQ

### Q1: How many vehicles can run simultaneously?

**A**: The current version supports up to 500 vehicles. Beyond this, you may experience:
- Frontend rendering lag
- WebSocket message delay
- Increased backend memory usage

**Optimization recommendations**:
- Replace SVG with WebGL rendering (Phase 4)
- Implement a vehicle pool (cap maximum concurrency)

---

### Q2: How do I change the signal light duration?

**A**: Edit `backend/src/main/java/com/traffic/optimization/model/TrafficLight.java`:

```java
public class TrafficLight {
    private static final int GREEN_DURATION = 30; // modify here (seconds)
    private static final int RED_DURATION = 30;   // modify here (seconds)
}
```

Recompile to apply the change.

---

### Q3: Why is the efficiency value negative?

**A**: Efficiency should never be negative. If it is, possible causes:

1. **Vehicle travel time is 0**
   - Vehicle just spawned and hasn't moved yet
   - Division by 0 causes a calculation error

2. **Data anomaly**
   - Check backend logs
   - Look for exception stack traces

**Solution**:
```java
// Add a guard
public double calculateEfficiency() {
    if (travelTime <= 0) return 0;
    return (distance / travelTime) * 3600; // convert to km/h
}
```

---

### Q4: Can more cities be added?

**A**: Yes. See "Phase 3.1 Multi-city Support" in ROADMAP.md.

**Simplified steps**:
1. Create a new JSON file: `frontend/src/data/boston_ma.json`
2. Define nodes and edges
3. Add an option to the city selector
4. Load the new city data

---

### Q5: How do I export a video / GIF?

**A**: The current version has no built-in recording. Recommended tools:

1. **Screen recording software**:
   - Mac: QuickTime Player
   - Windows: Xbox Game Bar (Win+G)
   - Cross-platform: OBS Studio

2. **Browser extensions**:
   - Loom (Chrome extension)
   - Screen Recorder (Firefox extension)

3. **Convert to GIF**:
   - Using ffmpeg:
     ```bash
     ffmpeg -i simulation.mp4 -vf "fps=10,scale=800:-1" -loop 0 simulation.gif
     ```

---

### Q6: Does the system support real-time road data?

**A**: The current version uses simulated data. Future versions plan to support:

- **Phase 3.4**: cloud deployment with third-party API integration
- **Possible data sources**:
  - Google Maps Traffic API
  - OpenStreetMap real-time data
  - City transportation department open data

---

### Q7: How do I contribute code or report a bug?

**A**: Contributions are welcome!

**Reporting bugs**:
1. Visit GitHub Issues
2. Click "New Issue"
3. Provide the following:
   - OS and browser version
   - Steps to reproduce
   - Expected behavior vs actual behavior
   - Screenshots or logs

**Contributing code**:
1. Fork the repository
2. Create a branch: `git checkout -b feature/your-feature`
3. Commit: `git commit -m "Add some feature"`
4. Push: `git push origin feature/your-feature`
5. Create a Pull Request

---

## Glossary

| Term | Description |
|------|-------------|
| Node | A point in the road network — either an intersection or a boundary point |
| Edge | A road connecting two nodes |
| Traffic Flow | A group of vehicles traveling from an origin to a destination |
| Efficiency | Overall network operational efficiency (km/h) |
| Congestion Level | Segment capacity utilization (%) |
| Throughput | Number of vehicles processed per unit time |
| Signal Timing | Red/green light duration configuration |
| Adaptive Control | Adjusts signal timing based on real-time traffic |
| Heatmap | A visualization that represents values using color |
| WebSocket | A bi-directional real-time communication protocol |
| KPI | Key Performance Indicator |
| BPR | Bureau of Public Roads speed-flow model |
| Webster Formula | Optimal signal cycle calculation: C0 = (1.5L + 5) / (1 - Sigma(yi)) |
| Green Wave | Corridor-coordinated signal offsets so vehicles at design speed catch every green |

---

## Appendix

### A. Keyboard Shortcuts (planned)

| Shortcut | Function |
|----------|---------|
| Space | Play / pause simulation |
| R | Reset simulation |
| H | Toggle heatmap |
| F | Focus traffic flow form |
| E | Open export panel |
| ? | Show help |

### B. Color Coding Reference

**Status colors**:
- Green (#10B981): good / success
- Yellow (#F59E0B): warning / moderate
- Red (#EF4444): error / poor
- Blue (#3B82F6): information / neutral
- Purple (#8B5CF6): special / advanced

**Node types**:
- Blue circle: intersection
- Red square: boundary point

### C. Data Format Reference

**CSV export format**:
- Encoding: UTF-8
- Delimiter: comma (,)
- Line ending: LF (\n)
- Quote escaping: double-quote ("")

**JSON export format**:
- Indentation: 2 spaces
- Encoding: UTF-8
- Date format: ISO 8601 (YYYY-MM-DDTHH:mm:ss)

---

## Contact

**Authors**:
- Chengkun Liao
- Mingjie Shen

**Repository**: [GitHub link]

**Feedback**: [email address]

**Course**: INFO6205 - Program Structure and Algorithms
**Semester**: Spring 2025
**School**: Northeastern University

---

**Document Version**: 1.0
**Published**: 2025-03-15
**Next Update**: TBD

---

Thank you for using the Traffic Signal Optimization System!

If you have any questions or suggestions, please feel free to reach out.
