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
    // Connect WebSocket
    websocketService.connect(
      () => {
        console.log('WebSocket connected');
        setConnected(true);

        // Subscribe to simulation state updates
        websocketService.subscribe('/topic/simulation', (data) => {
          console.log('Received simulation data:', data);
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

        // Subscribe to performance metrics updates
        websocketService.subscribe('/topic/metrics', (data) => {
          console.log('Received metrics data:', data);
          setMetrics(data);
        });
      },
      (error) => {
        console.error('WebSocket connection failed:', error);
        setConnected(false);
      }
    );

    // Periodically fetch efficiency trend data
    const intervalId = setInterval(async () => {
      try {
        const response = await simulationAPI.getEfficiencyTrend(50);
        setEfficiencyTrend(response.data);
      } catch (error) {
        console.error('Failed to fetch efficiency trend:', error);
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
        <h1>Traffic Signal Optimization System</h1>
        <div className="connection-status">
          <span className={`status-dot ${connected ? 'connected' : 'disconnected'}`}></span>
          <span>{connected ? 'Connected' : 'Disconnected'}</span>
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
        <p>Traffic Signal Optimization System - INFO6205 Final Project</p>
        <p>Authors: Chengkun Liao, Mingjie Shen</p>
      </footer>
    </div>
  );
}

export default App;
