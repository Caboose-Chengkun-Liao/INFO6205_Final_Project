# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a traffic signal optimization project (INFO6205 course final project) that improves traffic efficiency by optimizing signal control at intersections. Project authors: Chengkun Liao and Mingjie Shen.

**Core goal**: Simulate traffic flow and signal control based on real map data, and maximize traffic throughput by optimizing signal timing.

## Core Data Structures

### Weighted Directed Graph
- **Nodes**: represent intersections / junctions, each equipped with a traffic light
  - Numerically labeled nodes (1, 2, 3...): internal intersections
  - Alphabetically labeled nodes (A, B, C...): boundary entry / exit points
- **Edges**: represent roads; weight is road distance
- **Assumptions**:
  - All roads share the same unit capacity (n cars/km)
  - All roads share the same speed limit
  - Road capacity depends only on distance

### Traffic Flow Object
Each flow object contains the following attributes:
- `flowID`: unique identifier
- `entryPoint`: entry point
- `destination`: destination
- `numberOfCars`: number of vehicles
- `travelTimeCounter`: travel time counter

**Routing strategy**: All traffic flows travel between entry and destination using the shortest-path algorithm (Dijkstra).

## System Architecture

### 1. Road Network Layer
- Implements a weighted directed graph
- Manages the relationship between nodes (intersections) and edges (roads)
- Stores road distance, capacity, and other attributes

### 2. Traffic Flow Simulation Layer
- Creates and manages traffic flow objects
- Implements the shortest-path algorithm for route calculation
- Tracks each flow's real-time position and state
- Simulates vehicle queuing and passing at intersections

### 3. Signal Control Layer
- Manages the traffic light state at each intersection
- Dynamically adjusts signal timing based on traffic volume
- Implements signal optimization algorithms

### 4. Performance Evaluation Layer
Calculates the traffic efficiency metric E (evaluated once per hour):

```
E = Sigma(Ni x Li / ti) / Sigma(Ni)
```

Where:
- E: efficiency value
- Ni: number of vehicles in flow i
- Li: road length
- ti: time for flow i to pass between two intersections

**Optimization goal**: maximize E by adjusting the ti value through signal control

## Development Guidelines

### Suggested Code Organization
```
src/
|-- graph/              # Graph structure implementation
|   |-- Node.java       # Intersection node
|   |-- Edge.java       # Road edge
|   `-- Graph.java      # Graph main class
|-- traffic/            # Traffic flow simulation
|   |-- TrafficFlow.java
|   `-- FlowManager.java
|-- signal/             # Signal control
|   |-- TrafficLight.java
|   `-- SignalController.java
|-- algorithm/          # Algorithm implementations
|   |-- ShortestPath.java
|   `-- SignalOptimizer.java
|-- simulation/         # Simulation engine
|   `-- Simulator.java
`-- metrics/            # Performance metrics
    `-- EfficiencyCalculator.java
```

### Key Implementation Points

1. **Shortest-path algorithm**: implement Dijkstra for traffic flow routing

2. **Time-step simulation**:
   - Use discrete event simulation or fixed time steps
   - Update all traffic flow positions every time step
   - Handle intersection signal light state changes

3. **Signal optimization logic**:
   - Account for traffic patterns at different times (especially peak hours)
   - Adjust signal timing based on real-time / historical traffic data
   - Balance coordinated control across multiple intersections

4. **Efficiency calculation**:
   - Collect Ni, Li, ti for all traffic flows each hour
   - Calculate the aggregate efficiency metric E
   - Record and compare efficiency under different control strategies

### Testing Recommendations

- **Unit tests**: graph structure operations, shortest-path algorithm, efficiency formula
- **Integration tests**: full path simulation of a traffic flow through the network
- **Performance tests**: system response under large-scale traffic flows
- **Scenario tests**: peak hours, off-peak hours, and other traffic patterns

## Project Status

**Current phase**: Implementation complete — full Spring Boot + React system deployed

**Completed**:
- Problem definition and requirements analysis
- Data structure design
- Architecture planning
- Full backend implementation (graph, algorithms, simulation engine, signal control, REST API, WebSocket)
- Full frontend implementation (map visualization, dashboards, heatmap, data export)

**Remaining**:
- Performance testing at large scale
- Additional integration tests
- Cloud deployment (Phase 3)
