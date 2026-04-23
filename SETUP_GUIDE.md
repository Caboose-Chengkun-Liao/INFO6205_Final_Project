# Project Setup and Usage Guide

## Current Project Status

The following core features have been implemented:

### Completed Backend Features

1. **Data Models** (`backend/src/main/java/com/traffic/optimization/model/`)
   - `Node.java` - Intersection / node model
   - `Edge.java` - Road / edge model (includes dynamic speed adjustment algorithm)
   - `Graph.java` - Road network graph
   - `TrafficFlow.java` - Traffic flow model
   - `TrafficLight.java` - Signal light model
   - `NodeType.java` - Node type enum

2. **Algorithm Implementations** (`backend/src/main/java/com/traffic/optimization/algorithm/`)
   - `DijkstraAlgorithm.java` - Shortest path algorithm

3. **Business Logic Services** (`backend/src/main/java/com/traffic/optimization/service/`)
   - `SimulationEngine.java` - Simulation engine
   - `FlowManager.java` - Traffic flow manager
   - `SignalController.java` - Signal light controller
   - `EfficiencyCalculator.java` - Efficiency calculator

4. **REST API Controllers** (`backend/src/main/java/com/traffic/optimization/controller/`)
   - `SimulationController.java` - Simulation control API

5. **WebSocket Configuration** (`backend/src/main/java/com/traffic/optimization/`)
   - `WebSocketConfig.java` - WebSocket configuration
   - `SimulationWebSocketHandler.java` - Real-time data push

6. **Tests** (`backend/src/test/java/`)
   - `DijkstraAlgorithmTest.java` - Dijkstra algorithm unit tests

### Completed Frontend Features

1. **React Components** (`frontend/src/components/`)
   - `ControlPanel.jsx` - Simulation control panel
   - `MetricsDisplay.jsx` - Performance metrics display
   - `MetricsChart.jsx` - Efficiency trend chart

2. **Service Layer** (`frontend/src/services/`)
   - `api.js` - REST API client
   - `websocket.js` - WebSocket client

3. **Main Application**
   - `App.jsx` - Main application component
   - `App.css` - Stylesheet (desktop-optimized; mobile responsive design removed)
   - `index.css` - Global styles (full-width layout)

---

## Known Issues

### Lombok Compilation Problem

The backend uses Lombok annotations (`@Data`, `@Getter`, etc.) to reduce boilerplate. If you encounter compilation errors about missing getter / setter methods, follow the steps below:

#### Option 1: Configure Lombok Support in Your IDE

**IntelliJ IDEA:**
1. Install the Lombok plugin: `Preferences` -> `Plugins` -> search "Lombok" -> Install
2. Enable annotation processing: `Preferences` -> `Build, Execution, Deployment` -> `Compiler` -> `Annotation Processors` -> check "Enable annotation processing"
3. Re-import the Maven project

**Eclipse:**
1. Download `lombok.jar`
2. Run `java -jar lombok.jar`
3. Select the Eclipse installation directory
4. Restart Eclipse

#### Option 2: Remove the Lombok Dependency (recommended for quick testing)

If you just want to run the project quickly, manually add getters and setters to each class that uses `@Data`.

---

## Startup Steps

### 1. Start the Backend

```bash
cd backend

# Skip tests and run
mvn spring-boot:run -DskipTests

# Or build first, then run
mvn clean install -DskipTests
mvn spring-boot:run
```

The backend starts at `http://localhost:8080`

### 2. Start the Frontend

```bash
cd frontend

# Install dependencies (first run only)
npm install

# Start the development server
npm run dev
```

The frontend starts at `http://localhost:5173`

---

## Usage Instructions

### 1. Initialize the Simulation

1. Open a browser and go to `http://localhost:5173`
2. Click the "Initialize" button — the system creates the default road network
3. Wait for initialization to complete

### 2. Create Traffic Flows

Use the API or the frontend UI to create traffic flows:

```bash
curl -X POST http://localhost:8080/api/simulation/flows \
  -H "Content-Type: application/json" \
  -d '{
    "entryPoint": "A",
    "destination": "D",
    "numberOfCars": 100
  }'
```

### 3. Start the Simulation

Click the "Start" button. The system will:
- Update traffic flow positions every second
- Update signal light states every second
- Calculate performance metrics every 5 seconds
- Push data to the frontend in real time

### 4. View Real-time Data

The frontend displays in real time:
- Simulation state and elapsed time
- Performance metrics: efficiency, throughput, average speed, etc.
- Efficiency trend chart over time

---

## API Usage Examples

### Get Simulation Status

```bash
curl http://localhost:8080/api/simulation/status
```

### Get Performance Metrics

```bash
curl http://localhost:8080/api/simulation/metrics
```

### Get Efficiency Trend

```bash
curl http://localhost:8080/api/simulation/efficiency/trend?count=50
```

### Set Signal Optimization Mode

```bash
curl -X POST "http://localhost:8080/api/simulation/signals/mode?mode=TRAFFIC_ADAPTIVE"
```

Available modes:
- `FIXED_TIME` - Fixed timing
- `TRAFFIC_ADAPTIVE` - Traffic-adaptive (Webster formula)
- `GREEN_WAVE` - Green wave corridor coordination

---

## Next Development Steps

1. **Implement default map data**
   - Add a sample road network in `SimulationController.createDefaultGraph()`
   - Or load real map data from a configuration file or database

2. **Enhance frontend visualization**
   - Add an interactive map showing the road network
   - Display vehicle positions and signal light states in real time
   - Support manual adjustment of signal light parameters

3. **Improve test coverage**
   - Add more unit tests
   - Integration tests
   - End-to-end tests

4. **Performance optimization**
   - Stress test with large-scale traffic networks
   - Optimize algorithm performance
   - Add database persistence

---

## Troubleshooting

### Problem: Frontend cannot connect via WebSocket

**Solution:**
- Ensure the backend is running
- Check the CORS configuration
- Look at the browser console for error messages

### Problem: Maven build fails

**Solution:**
- Ensure the JDK version is 17 or higher
- Clean Maven: `mvn clean`
- Delete the cache under `~/.m2/repository` and re-download dependencies

### Problem: Frontend npm install fails

**Solution:**
- Ensure Node.js version is 16 or higher
- Clear the npm cache: `npm cache clean --force`
- Delete `node_modules` and `package-lock.json`, then reinstall

---

## Contact

For questions, please contact the project authors:
- Chengkun Liao
- Mingjie Shen
