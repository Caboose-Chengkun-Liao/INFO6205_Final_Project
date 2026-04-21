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
          <div style={{ flex: 1.4 }}>
            <ErrorBoundary title="Signal control error">
              <SignalControlPanel onSignalsUpdate={handleSignalsUpdate} />
            </ErrorBoundary>
          </div>
          <div style={{ flex: 1 }}>
            <div style={{
              background: '#FFFFFF',
              borderRadius: 24,
              padding: '28px 32px',
              boxShadow: '0 2px 8px rgba(0,0,0,0.04)',
              border: '1px solid rgba(0,0,0,0.06)',
              height: '100%',
              display: 'flex',
              flexDirection: 'column',
              justifyContent: 'space-between',
            }}>
              <div>
                <h2 style={{ margin: 0, fontSize: 22, fontWeight: 600, color: '#1D1D1F', letterSpacing: '-0.26px' }}>
                  Algorithm Benchmark
                </h2>
                <p style={{ margin: '4px 0 16px 0', fontSize: 13, color: '#86868B' }}>
                  Run Fixed / Adaptive / Green Wave in parallel on identical traffic
                </p>
              </div>
              <button
                onClick={() => setShowComparison(true)}
                style={{
                  background: 'linear-gradient(135deg, #0071E3, #0055b3)',
                  color: '#fff',
                  border: 'none',
                  borderRadius: 12,
                  padding: '14px 20px',
                  fontSize: 15,
                  fontWeight: 600,
                  cursor: 'pointer',
                  width: '100%',
                  marginBottom: 8,
                  boxShadow: '0 4px 14px rgba(0,113,227,0.25)',
                }}
              >
                Compare 3 Modes Side by Side
              </button>
              <button
                onClick={() => setShowFlowDiagrams(true)}
                style={{
                  background: '#F5F5F7',
                  color: '#1D1D1F',
                  border: '1px solid rgba(0,0,0,0.06)',
                  borderRadius: 12,
                  padding: '12px 20px',
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
