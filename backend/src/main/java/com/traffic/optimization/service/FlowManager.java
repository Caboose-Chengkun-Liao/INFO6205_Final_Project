package com.traffic.optimization.service;

import com.traffic.optimization.algorithm.DijkstraAlgorithm;
import com.traffic.optimization.model.*;
import lombok.Getter;
import org.springframework.stereotype.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 交通流管理器 - 负责创建、管理和更新所有交通流
 *
 * @author Chengkun Liao, Mingjie Shen
 */
@Service
@Getter
public class FlowManager {

    private static final Logger log = LoggerFactory.getLogger(FlowManager.class);

    /**
     * 道路网络图
     */
    private Graph graph;

    /**
     * 所有活跃的交通流
     */
    private Map<String, TrafficFlow> activeFlows;

    /**
     * 已完成的交通流
     */
    private Map<String, TrafficFlow> completedFlows;

    /**
     * 流ID生成器
     */
    private AtomicInteger flowIdGenerator;

    /**
     * 构造函数
     */
    public FlowManager() {
        this.activeFlows = new ConcurrentHashMap<>();
        this.completedFlows = new ConcurrentHashMap<>();
        this.flowIdGenerator = new AtomicInteger(1);
    }

    /**
     * 设置道路网络图
     */
    public void setGraph(Graph graph) {
        this.graph = graph;
    }

    /**
     * 创建新的交通流
     *
     * @param entryPoint 入口节点ID
     * @param destination 目的地节点ID
     * @param numberOfCars 车辆数量
     * @return 创建的交通流
     */
    public TrafficFlow createFlow(String entryPoint, String destination, int numberOfCars) {
        Node entryNode = graph.getNode(entryPoint);
        Node destNode = graph.getNode(destination);

        if (entryNode == null || destNode == null) {
            throw new IllegalArgumentException("无效的入口或目的地节点");
        }

        // 生成流ID
        String flowId = "FLOW-" + flowIdGenerator.getAndIncrement();

        // 创建交通流
        TrafficFlow flow = new TrafficFlow(flowId, entryNode, destNode, numberOfCars);

        // 计算最短路径
        List<Node> path = DijkstraAlgorithm.findShortestPath(graph, entryNode, destNode);

        if (path == null || path.isEmpty()) {
            throw new IllegalArgumentException("无法找到从 " + entryPoint + " 到 " + destination + " 的路径");
        }

        flow.setPath(path);
        flow.setState(TrafficFlow.FlowState.WAITING);

        // 添加到活跃流列表
        activeFlows.put(flowId, flow);

        return flow;
    }

    /**
     * 批量创建交通流
     */
    public List<TrafficFlow> createMultipleFlows(List<FlowRequest> requests) {
        List<TrafficFlow> flows = new ArrayList<>();

        for (FlowRequest request : requests) {
            try {
                TrafficFlow flow = createFlow(
                    request.getEntryPoint(),
                    request.getDestination(),
                    request.getNumberOfCars()
                );
                flows.add(flow);
            } catch (Exception e) {
                log.warn("创建交通流失败: {}", e.getMessage());
            }
        }

        return flows;
    }

    /**
     * 更新所有活跃的交通流（每个时间步调用）
     *
     * @param deltaTime 时间增量（秒）
     */
    public void updateFlows(double deltaTime) {
        List<String> toRemove = new ArrayList<>();

        for (TrafficFlow flow : activeFlows.values()) {
            // 更新旅行时间
            flow.updateTravelTime(deltaTime);

            // 检查是否完成
            if (flow.isCompleted()) {
                log.debug("检测到完成的流: {}", flow.getFlowId());
                toRemove.add(flow.getFlowId());
                completedFlows.put(flow.getFlowId(), flow);
            }
        }

        // 移除已完成的流
        for (String flowId : toRemove) {
            activeFlows.remove(flowId);
        }

        if (!toRemove.isEmpty()) {
            log.debug("移除已完成流: {} 个, 当前活跃={}, 已完成={}",
                toRemove.size(), activeFlows.size(), completedFlows.size());
        }
    }

    /**
     * 获取指定节点的等待流数量
     */
    public int getWaitingFlowsAtNode(Node node) {
        int count = 0;
        for (TrafficFlow flow : activeFlows.values()) {
            if (flow.getCurrentNode() != null &&
                flow.getCurrentNode().equals(node) &&
                flow.getState() == TrafficFlow.FlowState.BLOCKED) {
                count++;
            }
        }
        return count;
    }

    /**
     * 获取指定边上的流
     */
    public List<TrafficFlow> getFlowsOnEdge(Edge edge) {
        List<TrafficFlow> flows = new ArrayList<>();
        for (TrafficFlow flow : activeFlows.values()) {
            if (flow.getCurrentEdge() != null &&
                flow.getCurrentEdge().equals(edge)) {
                flows.add(flow);
            }
        }
        return flows;
    }

    /**
     * 获取所有活跃流的列表
     */
    public List<TrafficFlow> getActiveFlowsList() {
        return new ArrayList<>(activeFlows.values());
    }

    /**
     * 获取所有已完成流的列表
     */
    public List<TrafficFlow> getCompletedFlowsList() {
        return new ArrayList<>(completedFlows.values());
    }

    /**
     * 获取总流数量
     */
    public int getTotalFlowCount() {
        return activeFlows.size() + completedFlows.size();
    }

    /**
     * 清空所有流
     */
    public void clearAllFlows() {
        activeFlows.clear();
        completedFlows.clear();
        flowIdGenerator.set(1);
    }

    /**
     * 打印统计信息
     */
    public void printStatistics() {
        log.info("交通流统计 - 活跃: {}, 已完成: {}, 总计: {}",
            activeFlows.size(), completedFlows.size(), getTotalFlowCount());
    }

    /**
     * 交通流请求类
     */
    @Getter
    public static class FlowRequest {
        private String entryPoint;
        private String destination;
        private int numberOfCars;

        public FlowRequest(String entryPoint, String destination, int numberOfCars) {
            this.entryPoint = entryPoint;
            this.destination = destination;
            this.numberOfCars = numberOfCars;
        }
    }
}
