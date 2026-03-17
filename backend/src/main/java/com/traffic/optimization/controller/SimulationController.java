package com.traffic.optimization.controller;

import com.traffic.optimization.model.Edge;
import com.traffic.optimization.model.Graph;
import com.traffic.optimization.model.Node;
import com.traffic.optimization.model.NodeType;
import com.traffic.optimization.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 仿真控制器 - RESTful API
 *
 * @author Chengkun Liao, Mingjie Shen
 */
@RestController
@RequestMapping("/api/simulation")
@CrossOrigin(origins = "*")
public class SimulationController {

    @Autowired
    private SimulationEngine simulationEngine;

    @Autowired
    private FlowManager flowManager;

    @Autowired
    private SignalController signalController;

    @Autowired
    private EfficiencyCalculator efficiencyCalculator;

    /**
     * 初始化仿真（使用默认地图）
     */
    @PostMapping("/initialize")
    public ResponseEntity<Map<String, Object>> initialize() {
        try {
            Graph graph = createDefaultGraph();
            simulationEngine.initialize(graph);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "仿真已初始化");
            response.put("graph", getGraphInfo(graph));

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * 开始仿真
     */
    @PostMapping("/start")
    public ResponseEntity<Map<String, Object>> start() {
        simulationEngine.start();
        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "仿真已启动",
            "state", simulationEngine.getState()
        ));
    }

    /**
     * 暂停仿真
     */
    @PostMapping("/pause")
    public ResponseEntity<Map<String, Object>> pause() {
        simulationEngine.pause();
        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "仿真已暂停",
            "state", simulationEngine.getState()
        ));
    }

    /**
     * 停止仿真
     */
    @PostMapping("/stop")
    public ResponseEntity<Map<String, Object>> stop() {
        simulationEngine.stop();
        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "仿真已停止",
            "state", simulationEngine.getState()
        ));
    }

    /**
     * 重置仿真
     */
    @PostMapping("/reset")
    public ResponseEntity<Map<String, Object>> reset() {
        simulationEngine.reset();
        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "仿真已重置",
            "state", simulationEngine.getState()
        ));
    }

    /**
     * 执行单步
     */
    @PostMapping("/step")
    public ResponseEntity<Map<String, Object>> step() {
        simulationEngine.step();
        return ResponseEntity.ok(Map.of(
            "success", true,
            "currentTime", simulationEngine.getCurrentTime()
        ));
    }

    /**
     * 获取仿真状态
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        int activeSize = flowManager.getActiveFlowsList().size();
        int completedSize = flowManager.getCompletedFlowsList().size();

        System.out.println("DEBUG Controller.getStatus: activeFlows=" + activeSize +
            " completedFlows=" + completedSize);

        Map<String, Object> status = new HashMap<>();
        status.put("state", simulationEngine.getState());
        status.put("currentTime", simulationEngine.getCurrentTime());
        status.put("metrics", simulationEngine.getCurrentMetrics());
        status.put("activeFlows", activeSize);
        status.put("completedFlows", completedSize);

        return ResponseEntity.ok(status);
    }

    /**
     * 获取性能指标
     */
    @GetMapping("/metrics")
    public ResponseEntity<EfficiencyCalculator.PerformanceMetrics> getMetrics() {
        return ResponseEntity.ok(simulationEngine.getCurrentMetrics());
    }

    /**
     * 获取效率趋势
     */
    @GetMapping("/efficiency/trend")
    public ResponseEntity<List<EfficiencyCalculator.EfficiencyRecord>> getEfficiencyTrend(
            @RequestParam(defaultValue = "50") int count) {
        return ResponseEntity.ok(efficiencyCalculator.getEfficiencyTrend(count));
    }

    /**
     * 创建交通流
     */
    @PostMapping("/flows")
    public ResponseEntity<Map<String, Object>> createFlow(@RequestBody FlowManager.FlowRequest request) {
        try {
            var flow = flowManager.createFlow(
                request.getEntryPoint(),
                request.getDestination(),
                request.getNumberOfCars()
            );

            return ResponseEntity.ok(Map.of(
                "success", true,
                "flowId", flow.getFlowId()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * 获取道路网络图数据（用于前端地图渲染）
     */
    @GetMapping("/graph")
    public ResponseEntity<Map<String, Object>> getGraph() {
        Graph graph = simulationEngine.getGraph();
        if (graph == null) {
            return ResponseEntity.badRequest()
                .body(Map.of("success", false, "error", "仿真未初始化"));
        }

        Map<String, Object> graphData = new HashMap<>();

        // 节点数据
        List<Map<String, Object>> nodes = new ArrayList<>();
        for (Node node : graph.getAllNodes()) {
            Map<String, Object> nodeData = new HashMap<>();
            nodeData.put("id", node.getId());
            nodeData.put("name", node.getName());
            nodeData.put("type", node.getType().toString());
            nodeData.put("x", node.getX());
            nodeData.put("y", node.getY());
            nodes.add(nodeData);
        }

        // 边数据
        List<Map<String, Object>> edges = new ArrayList<>();
        for (Node node : graph.getAllNodes()) {
            for (Edge edge : node.getOutgoingEdges()) {
                Map<String, Object> edgeData = new HashMap<>();
                edgeData.put("id", edge.getId());
                edgeData.put("from", edge.getFromNode().getId());
                edgeData.put("to", edge.getToNode().getId());
                edgeData.put("distance", edge.getDistance());
                edgeData.put("currentLoad", edge.getCurrentVehicleCount());
                edges.add(edgeData);
            }
        }

        graphData.put("nodes", nodes);
        graphData.put("edges", edges);

        return ResponseEntity.ok(graphData);
    }

    /**
     * 获取车辆位置信息（用于地图可视化）
     */
    @GetMapping("/vehicles")
    public ResponseEntity<List<Map<String, Object>>> getVehiclePositions() {
        List<Map<String, Object>> vehicles = new ArrayList<>();

        for (var flow : flowManager.getActiveFlowsList()) {
            if (flow.getCurrentEdge() != null) {
                Map<String, Object> vehicleData = new HashMap<>();
                vehicleData.put("flowId", flow.getFlowId());
                vehicleData.put("numberOfCars", flow.getNumberOfCars());
                vehicleData.put("state", flow.getState().toString());

                // 当前位置信息
                Edge currentEdge = flow.getCurrentEdge();
                vehicleData.put("currentEdge", currentEdge.getId());
                vehicleData.put("from", currentEdge.getFromNode().getId());
                vehicleData.put("to", currentEdge.getToNode().getId());

                // 计算在边上的进度（0-1）
                double progress = Math.min(1.0,
                    flow.getTimeOnCurrentEdge() / (currentEdge.getIdealTravelTime() * 60));
                vehicleData.put("progress", progress);

                vehicles.add(vehicleData);
            }
        }

        return ResponseEntity.ok(vehicles);
    }

    /**
     * 获取所有信号灯状态
     */
    @GetMapping("/signals")
    public ResponseEntity<List<SignalController.SignalStatus>> getSignalStatuses() {
        return ResponseEntity.ok(signalController.getAllSignalStatuses());
    }

    /**
     * 设置信号优化模式
     */
    @PostMapping("/signals/mode")
    public ResponseEntity<Map<String, Object>> setSignalMode(
            @RequestParam SignalController.OptimizationMode mode) {
        signalController.setOptimizationMode(mode);
        return ResponseEntity.ok(Map.of(
            "success", true,
            "mode", mode
        ));
    }

    /**
     * 创建默认道路网络图 - 基于弗吉尼亚阿灵顿地区真实路网
     */
    private Graph createDefaultGraph() {
        Graph graph = new Graph();

        // ==================== 边界节点（交通流起点/终点） ====================
        // 北部边界
        Node boundaryN1 = new Node("N1", "Lee Hwy North Entry", NodeType.BOUNDARY, 2.0, 8.0);
        Node boundaryN2 = new Node("N2", "Washington Blvd North Entry", NodeType.BOUNDARY, 5.0, 8.0);

        // 南部边界
        Node boundaryS1 = new Node("S1", "Columbia Pike South Entry", NodeType.BOUNDARY, 2.0, 0.0);
        Node boundaryS2 = new Node("S2", "Arlington Blvd South Entry", NodeType.BOUNDARY, 6.0, 0.0);

        // 东部边界
        Node boundaryE1 = new Node("E1", "Key Bridge Entry", NodeType.BOUNDARY, 8.0, 6.0);
        Node boundaryE2 = new Node("E2", "Memorial Bridge Entry", NodeType.BOUNDARY, 8.0, 4.0);

        // 西部边界
        Node boundaryW1 = new Node("W1", "Route 50 West Entry", NodeType.BOUNDARY, 0.0, 4.0);
        Node boundaryW2 = new Node("W2", "Lee Hwy West Entry", NodeType.BOUNDARY, 0.0, 6.0);

        graph.addNode(boundaryN1);
        graph.addNode(boundaryN2);
        graph.addNode(boundaryS1);
        graph.addNode(boundaryS2);
        graph.addNode(boundaryE1);
        graph.addNode(boundaryE2);
        graph.addNode(boundaryW1);
        graph.addNode(boundaryW2);

        // ==================== 主要路口节点 ====================
        // Clarendon区域 (阿灵顿商业中心)
        Node clarendon = new Node("1", "Clarendon Blvd & Wilson Blvd", NodeType.INTERSECTION, 4.0, 6.0);

        // Courthouse区域
        Node courthouse = new Node("2", "Courthouse Rd & Wilson Blvd", NodeType.INTERSECTION, 5.5, 6.5);

        // Ballston区域
        Node ballston = new Node("3", "Fairfax Dr & Wilson Blvd", NodeType.INTERSECTION, 2.5, 6.5);

        // Rosslyn区域 (靠近Key Bridge)
        Node rosslyn = new Node("4", "Fort Myer Dr & Wilson Blvd", NodeType.INTERSECTION, 6.5, 6.0);

        // Pentagon City区域
        Node pentagonCity = new Node("5", "Army Navy Dr & S Hayes St", NodeType.INTERSECTION, 6.0, 2.5);

        // Crystal City区域
        Node crystalCity = new Node("6", "Crystal Dr & 15th St", NodeType.INTERSECTION, 7.0, 3.0);

        // Columbia Pike主干道路口
        Node columbiaPike1 = new Node("7", "Columbia Pike & S Glebe Rd", NodeType.INTERSECTION, 3.0, 2.0);
        Node columbiaPike2 = new Node("8", "Columbia Pike & S Walter Reed Dr", NodeType.INTERSECTION, 4.5, 2.5);

        // Arlington Blvd (Route 50)路口
        Node route50_1 = new Node("9", "Arlington Blvd & N Courthouse Rd", NodeType.INTERSECTION, 5.0, 4.5);
        Node route50_2 = new Node("10", "Arlington Blvd & N Highland St", NodeType.INTERSECTION, 3.5, 4.0);

        // Lee Highway路口
        Node leeHwy1 = new Node("11", "Lee Hwy & N Fillmore St", NodeType.INTERSECTION, 2.0, 7.0);
        Node leeHwy2 = new Node("12", "Lee Hwy & N Lynn St", NodeType.INTERSECTION, 5.5, 7.5);

        graph.addNode(clarendon);
        graph.addNode(courthouse);
        graph.addNode(ballston);
        graph.addNode(rosslyn);
        graph.addNode(pentagonCity);
        graph.addNode(crystalCity);
        graph.addNode(columbiaPike1);
        graph.addNode(columbiaPike2);
        graph.addNode(route50_1);
        graph.addNode(route50_2);
        graph.addNode(leeHwy1);
        graph.addNode(leeHwy2);

        // ==================== 创建道路（双向边） ====================

        // Wilson Blvd主干道 (东西向，穿过Clarendon, Courthouse, Rosslyn)
        graph.addBidirectionalEdge("E1", "E2", ballston, clarendon, 1.8);
        graph.addBidirectionalEdge("E3", "E4", clarendon, courthouse, 1.5);
        graph.addBidirectionalEdge("E5", "E6", courthouse, rosslyn, 1.2);

        // Lee Highway (东西向)
        graph.addBidirectionalEdge("E7", "E8", leeHwy1, ballston, 0.8);
        graph.addBidirectionalEdge("E9", "E10", ballston, clarendon, 1.0);
        graph.addBidirectionalEdge("E11", "E12", courthouse, leeHwy2, 0.9);

        // Arlington Blvd / Route 50 (东西向主干道)
        graph.addBidirectionalEdge("E13", "E14", route50_2, route50_1, 1.6);
        graph.addBidirectionalEdge("E15", "E16", route50_1, rosslyn, 2.0);

        // Columbia Pike (东南向主干道)
        graph.addBidirectionalEdge("E17", "E18", columbiaPike1, columbiaPike2, 1.8);
        graph.addBidirectionalEdge("E19", "E20", columbiaPike2, pentagonCity, 1.5);

        // 南北向连接道路
        graph.addBidirectionalEdge("E21", "E22", clarendon, route50_1, 1.8);
        graph.addBidirectionalEdge("E23", "E24", route50_2, columbiaPike1, 2.2);
        graph.addBidirectionalEdge("E25", "E26", rosslyn, crystalCity, 3.5);
        graph.addBidirectionalEdge("E27", "E28", crystalCity, pentagonCity, 0.8);
        graph.addBidirectionalEdge("E29", "E30", pentagonCity, route50_1, 2.0);

        // ==================== 边界节点连接 ====================

        // 北部边界连接
        graph.addBidirectionalEdge("E31", "E32", boundaryN1, leeHwy1, 0.5);
        graph.addBidirectionalEdge("E33", "E34", boundaryN2, leeHwy2, 0.6);
        graph.addBidirectionalEdge("E35", "E36", boundaryW2, leeHwy1, 0.3);

        // 南部边界连接
        graph.addBidirectionalEdge("E37", "E38", boundaryS1, columbiaPike1, 0.4);
        graph.addBidirectionalEdge("E39", "E40", boundaryS2, pentagonCity, 0.5);

        // 东部边界连接 (通往DC)
        graph.addBidirectionalEdge("E41", "E42", boundaryE1, rosslyn, 0.8);
        graph.addBidirectionalEdge("E43", "E44", boundaryE2, crystalCity, 1.0);

        // 西部边界连接
        graph.addBidirectionalEdge("E45", "E46", boundaryW1, route50_2, 0.5);
        graph.addBidirectionalEdge("E47", "E48", boundaryW2, leeHwy1, 0.3);

        System.out.println("===========================================");
        System.out.println("已加载阿灵顿地区道路网络");
        System.out.println("主要区域: Clarendon, Courthouse, Ballston, Rosslyn");
        System.out.println("        Pentagon City, Crystal City, Columbia Pike");
        graph.printStatistics();
        System.out.println("===========================================");

        return graph;
    }

    /**
     * 获取图的基本信息
     */
    private Map<String, Object> getGraphInfo(Graph graph) {
        Map<String, Object> info = new HashMap<>();
        info.put("nodeCount", graph.getNodeCount());
        info.put("edgeCount", graph.getEdgeCount());
        info.put("intersectionCount", graph.getIntersectionNodes().size());
        info.put("boundaryCount", graph.getBoundaryNodes().size());
        return info;
    }
}
