import { useState, useEffect, useCallback } from 'react';
import ErrorBoundary from './components/ErrorBoundary';
import ControlPanel from './components/ControlPanel';
import MetricsChart from './components/MetricsChart';
import MapVisualization from './components/MapVisualization';
import TrafficScene3D from './components/TrafficScene3D';
import StatisticsDashboard from './components/StatisticsDashboard';
import SignalControlPanel from './components/SignalControlPanel';
import SignalGridPanel from './components/SignalGridPanel';
import AlgorithmComparison from './components/AlgorithmComparison';
import DemoController from './components/DemoController';
import ComparisonView from './components/ComparisonView';
import ModeFlowDiagrams from './components/ModeFlowDiagrams';
import websocketService from './services/websocket';
import { simulationAPI } from './services/api';
import './App.css';

function App() {
  const [simulationState, setSimulationState] = useState('STOPPED');
  const [currentTime, setCurrentTime] = useState(0);
  const [metrics, setMetrics] = useState(null);
  const [efficiencyTrend, setEfficiencyTrend] = useState([]);
  const [connected, setConnected] = useState(false);
  const [signals, setSignals] = useState([]);
  const [viewMode, setViewMode] = useState('3d');
  const [showComparison, setShowComparison] = useState(false);
  const [showFlowDiagrams, setShowFlowDiagrams] = useState(false);

  useEffect(() => {
    websocketService.connect(
      () => {
        setConnected(true);

        websocketService.subscribe('/topic/simulation', (data) => {
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

        websocketService.subscribe('/topic/metrics', (data) => {
          setMetrics(data);
        });
      },
      (error) => {
        console.error('WebSocket connection failed:', error);
        setConnected(false);
      }
    );

    const intervalId = setInterval(async () => {
      try {
        const response = await simulationAPI.getEfficiencyTrend(50);
        setEfficiencyTrend(response.data);
      } catch (error) {
        // Silent - trend data is non-critical
      }
    }, 5000);

    return () => {
      websocketService.disconnect();
      clearInterval(intervalId);
    };
  }, []);

  const handleStateChange = useCallback((newState) => {
    setSimulationState(newState);
  }, []);

  const handleSignalsUpdate = useCallback((newSignals) => {
    setSignals(newSignals);
  }, []);

  // Comparison mode — full-page 3-scene view
  if (showComparison) {
    return (
      <div className="app">
        <header className="app-header">
          <h1>Traffic Signal Optimization System</h1>
          <div className="connection-status">
            <span className={`status-dot ${connected ? 'connected' : 'disconnected'}`}></span>
            <span>{connected ? 'Connected' : 'Connecting...'}</span>
          </div>
        </header>
        <ComparisonView onBack={() => setShowComparison(false)} />
      </div>
    );
  }

  // Flow diagrams mode — full-page algorithm explanation
  if (showFlowDiagrams) {
    return (
      <div className="app">
        <header className="app-header">
          <h1>Traffic Signal Optimization System</h1>
          <div className="connection-status">
            <span className={`status-dot ${connected ? 'connected' : 'disconnected'}`}></span>
            <span>{connected ? 'Connected' : 'Connecting...'}</span>
          </div>
        </header>
        <ModeFlowDiagrams onBack={() => setShowFlowDiagrams(false)} />
      </div>
    );
  }

  return (
    <div className="app">
      <header className="app-header">
        <h1>Traffic Signal Optimization System</h1>
        <div className="connection-status">
          <span className={`status-dot ${connected ? 'connected' : 'disconnected'}`}></span>
          <span>{connected ? 'Connected' : 'Connecting...'}</span>
        </div>
      </header>

      <main className="app-main">
        <div className="section">
          <ErrorBoundary title="Control panel error">
            <ControlPanel
              onStateChange={handleStateChange}
              currentTime={currentTime}
              simulationState={simulationState}
            />
          </ErrorBoundary>
        </div>

        <div className="section" style={{ display: 'flex', gap: 16 }}>
          <div style={{ flex: 1 }}>
            <ErrorBoundary title="Signal control error">
              <SignalControlPanel onSignalsUpdate={handleSignalsUpdate} />
            </ErrorBoundary>
          </div>
          <div style={{ flex: 1 }}>
            <ErrorBoundary title="Demo controller error">
              <DemoController onStateChange={handleStateChange} />
            </ErrorBoundary>
          </div>
          <div style={{ flex: 1 }}>
            <div style={{
              background: 'rgba(255,255,255,0.92)',
              backdropFilter: 'saturate(180%) blur(16px)',
              borderRadius: 12,
              padding: '12px 16px',
              boxShadow: '0 2px 12px rgba(0,0,0,0.08)',
            }}>
              <div style={{ fontWeight: 700, fontSize: 14, color: '#1D1D1F', marginBottom: 8 }}>
                Mode Comparison
              </div>
              <button
                onClick={() => setShowComparison(true)}
                style={{
                  background: 'linear-gradient(135deg, #667eea, #764ba2)',
                  color: '#fff',
                  border: 'none',
                  borderRadius: 8,
                  padding: '8px 20px',
                  fontSize: 13,
                  fontWeight: 600,
                  cursor: 'pointer',
                  width: '100%',
                  marginBottom: 6,
                }}
              >
                Compare 3 Modes Side by Side
              </button>
              <button
                onClick={() => setShowFlowDiagrams(true)}
                style={{
                  background: 'linear-gradient(135deg, #f093fb, #f5576c)',
                  color: '#fff',
                  border: 'none',
                  borderRadius: 8,
                  padding: '8px 20px',
                  fontSize: 13,
                  fontWeight: 600,
                  cursor: 'pointer',
                  width: '100%',
                }}
              >
                View Algorithm Flow Diagrams
              </button>
            </div>
          </div>
        </div>

        <div className="section">
          <ErrorBoundary title="Statistics error">
            <StatisticsDashboard />
          </ErrorBoundary>
        </div>

        <div className="section">
          <div style={{ display: 'flex', justifyContent: 'flex-end', marginBottom: 8 }}>
            <button
              onClick={() => setViewMode(viewMode === '3d' ? '2d' : '3d')}
              style={{
                border: 'none',
                borderRadius: 6,
                padding: '4px 14px',
                fontSize: 12,
                fontWeight: 600,
                cursor: 'pointer',
                background: viewMode === '3d' ? '#0071E3' : '#86868B',
                color: '#fff',
              }}
            >
              {viewMode === '3d' ? 'Switch to 2D' : 'Switch to 3D'}
            </button>
          </div>
          <ErrorBoundary title="Map visualization error">
            {viewMode === '3d'
              ? <TrafficScene3D signals={signals} />
              : <MapVisualization signals={signals} />
            }
          </ErrorBoundary>
        </div>

        <div className="section">
          <ErrorBoundary title="Algorithm comparison error">
            <AlgorithmComparison />
          </ErrorBoundary>
        </div>

        <div className="section">
          <ErrorBoundary title="Signal grid error">
            <SignalGridPanel signals={signals} />
          </ErrorBoundary>
        </div>

        <div className="section">
          <ErrorBoundary title="Chart error">
            <MetricsChart data={efficiencyTrend} />
          </ErrorBoundary>
        </div>
      </main>

      <footer className="app-footer">
        <p>Traffic Signal Optimization System — INFO6205 Final Project</p>
        <p>Authors: Chengkun Liao, Mingjie Shen</p>
      </footer>
    </div>
  );
}

export default App;
