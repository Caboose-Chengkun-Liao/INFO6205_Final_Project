import { useState, useEffect } from 'react';
import ControlPanel from './components/ControlPanel';
import MetricsDisplay from './components/MetricsDisplay';
import MetricsChart from './components/MetricsChart';
import MapVisualization from './components/MapVisualization';
import StatisticsDashboard from './components/StatisticsDashboard';
import websocketService from './services/websocket';
import { simulationAPI } from './services/api';
import './App.css';

function App() {
  const [simulationState, setSimulationState] = useState('STOPPED');
  const [currentTime, setCurrentTime] = useState(0);
  const [metrics, setMetrics] = useState(null);
  const [efficiencyTrend, setEfficiencyTrend] = useState([]);
  const [connected, setConnected] = useState(false);

  useEffect(() => {
    // 连接WebSocket
    websocketService.connect(
      () => {
        console.log('WebSocket已连接');
        setConnected(true);

        // 订阅仿真状态更新
        websocketService.subscribe('/topic/simulation', (data) => {
          console.log('收到仿真数据:', data);
          if (data.state) {
            setSimulationState(data.state);
          }
          if (data.currentTime !== undefined) {
            setCurrentTime(data.currentTime);
          }
          if (data.metrics) {
            setMetrics(data.metrics);
          }
        });

        // 订阅性能指标更新
        websocketService.subscribe('/topic/metrics', (data) => {
          console.log('收到指标数据:', data);
          setMetrics(data);
        });
      },
      (error) => {
        console.error('WebSocket连接失败:', error);
        setConnected(false);
      }
    );

    // 定期获取效率趋势数据
    const intervalId = setInterval(async () => {
      try {
        const response = await simulationAPI.getEfficiencyTrend(50);
        setEfficiencyTrend(response.data);
      } catch (error) {
        console.error('获取效率趋势失败:', error);
      }
    }, 5000);

    return () => {
      websocketService.disconnect();
      clearInterval(intervalId);
    };
  }, []);

  const handleStateChange = (newState) => {
    setSimulationState(newState);
  };

  return (
    <div className="app">
      <header className="app-header">
        <h1>交通信号优化系统</h1>
        <div className="connection-status">
          <span className={`status-dot ${connected ? 'connected' : 'disconnected'}`}></span>
          <span>{connected ? '已连接' : '未连接'}</span>
        </div>
      </header>

      <main className="app-main">
        <div className="section">
          <ControlPanel
            onStateChange={handleStateChange}
            currentTime={currentTime}
            simulationState={simulationState}
          />
        </div>

        <div className="section full-width">
          <StatisticsDashboard />
        </div>

        <div className="section full-width">
          <MapVisualization />
        </div>

        <div className="section">
          <MetricsDisplay metrics={metrics} />
        </div>

        <div className="section full-width">
          <MetricsChart data={efficiencyTrend} />
        </div>
      </main>

      <footer className="app-footer">
        <p>交通信号优化系统 - INFO6205 Final Project</p>
        <p>作者: Chengkun Liao, Mingjie Shen</p>
      </footer>
    </div>
  );
}

export default App;
